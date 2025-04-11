package com.tinysx.welcomeads.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HeadsUtil {
    private static final Map<String, ItemStack> headCache = new HashMap<>();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void getPlayerHead(String username, Consumer<ItemStack> callback) {

        if (headCache.containsKey(username)) {
            callback.accept(headCache.get(username).clone());
            return;
        }

        fetchTextureFromMojangAPI(username).thenAccept(texture -> {
            if (texture != null) {
                callback.accept(createHeadWithTexture(texture, username));
            } else {
                fetchTextureFromCraftHead(username).thenAccept(craftTexture -> {
                    if (craftTexture != null) {
                        callback.accept(createHeadWithTexture(craftTexture, username));
                    } else {
                        callback.accept(createDefaultHead());
                    }
                });
            }
        });
    }

    private static CompletableFuture<String> fetchTextureFromMojangAPI(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest uuidRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> uuidResponse = HTTP_CLIENT.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
                if (uuidResponse.statusCode() != 200)
                    return null;

                JsonObject uuidJson = JsonParser.parseString(uuidResponse.body()).getAsJsonObject();
                String uuid = uuidJson.get("id").getAsString();

                HttpRequest textureRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> textureResponse = HTTP_CLIENT.send(textureRequest,
                        HttpResponse.BodyHandlers.ofString());
                if (textureResponse.statusCode() != 200)
                    return null;

                JsonObject textureJson = JsonParser.parseString(textureResponse.body()).getAsJsonObject();
                return textureJson.getAsJsonArray("properties")
                        .get(0).getAsJsonObject()
                        .get("value").getAsString();
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static CompletableFuture<String> fetchTextureFromCraftHead(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://crafthead.net/profile/" + username))
                        .timeout(java.time.Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                    return null;

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.getAsJsonArray("properties")
                        .get(0).getAsJsonObject()
                        .get("value").getAsString();
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static ItemStack createHeadWithTexture(String texture, String username) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            try {
                org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(username);
                profile.getTextures().setSkin(getSkinUrlFromTexture(texture).toURL());
                meta.setOwnerProfile(profile);
            } catch (Exception e) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(username));
            }

            head.setItemMeta(meta);
            return head;
        } catch (Exception e) {
            return createDefaultHead();
        }
    }

    private static ItemStack createDefaultHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.randomUUID()));
        head.setItemMeta(meta);
        return head;
    }

    private static URI getSkinUrlFromTexture(String texture) {
        try {
            String decoded = new String(Base64.getDecoder().decode(texture));
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            String url = json.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();
            return URI.create(url);
        } catch (Exception e) {
            return URI.create(
                    "https://textures.minecraft.net/texture/d83c0d210a9abf5219886a7162eaaa9b4c8c326c3a9b8a7d9e7d9f5b5d5e5f5");
        }
    }
}