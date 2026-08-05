package com.tinysx.welcomeads.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class HeadsUtil {

    private static final int MAX_CACHE_SIZE = 500;
    private static final Map<String, ItemStack> HEAD_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, ItemStack>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ItemStack> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private HeadsUtil() {
    }

    public static void getPlayerHead(String username, Consumer<ItemStack> callback) {
        if (username == null || username.isEmpty()) {
            callback.accept(createDefaultHead());
            return;
        }

        String lowerUser = username.toLowerCase();
        synchronized (HEAD_CACHE) {
            if (HEAD_CACHE.containsKey(lowerUser)) {
                callback.accept(HEAD_CACHE.get(lowerUser).clone());
                return;
            }
        }

        fetchTextureFromMojangAPI(username).thenAccept(texture -> {
            if (texture != null) {
                ItemStack head = createHeadWithTexture(texture, username);
                HEAD_CACHE.put(lowerUser, head);
                callback.accept(head.clone());
            } else {
                fetchTextureFromCraftHead(username).thenAccept(craftTexture -> {
                    if (craftTexture != null) {
                        ItemStack head = createHeadWithTexture(craftTexture, username);
                        HEAD_CACHE.put(lowerUser, head);
                        callback.accept(head.clone());
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
                        .timeout(Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> uuidResponse = HTTP_CLIENT.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
                if (uuidResponse.statusCode() != 200) {
                    return null;
                }

                JsonObject uuidJson = JsonParser.parseString(uuidResponse.body()).getAsJsonObject();
                String uuid = uuidJson.get("id").getAsString();

                HttpRequest textureRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                        .timeout(Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> textureResponse = HTTP_CLIENT.send(textureRequest,
                        HttpResponse.BodyHandlers.ofString());
                if (textureResponse.statusCode() != 200) {
                    return null;
                }

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
                        .timeout(Duration.ofSeconds(3))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return null;
                }

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
            if (meta != null) {
                try {
                    PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), username);
                    profile.getTextures().setSkin(getSkinUrlFromTexture(texture).toURL());
                    meta.setOwnerProfile(profile);
                } catch (Exception e) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(username));
                }
                head.setItemMeta(meta);
            }
            return head;
        } catch (Exception e) {
            return createDefaultHead();
        }
    }

    public static ItemStack createDefaultHead() {
        return new ItemStack(Material.PLAYER_HEAD);
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
