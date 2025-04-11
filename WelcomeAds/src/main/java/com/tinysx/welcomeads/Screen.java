package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.tinysx.welcomeads.utils.CommandConverter;
import com.tinysx.welcomeads.utils.HeadsUtil;

import de.tr7zw.changeme.nbtapi.NBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public final class Screen {

    private final Map<Integer, String> slotToKeyMap = new HashMap<>();
    private final Map<String, List<String>> commandMap = new HashMap<>();

    public Inventory screenInventory;
    public String index;
    public String title;
    public String background;
    public Integer backgroundStay = 0;
    public Integer backgroundFadeout = 0;
    public ConfigurationSection itemsection;
    private final Config config;
    private final WelcomeInventoryHolder holder;
    private final WelcomeAds welcomeads = WelcomeAds.getPlugin(WelcomeAds.class);
    private final Map<Integer, ItemStack> preloadedItems = new HashMap<>();
    private boolean itemsPreloaded = false;

    public Screen(String index, Player player) {
        this.index = index;
        this.config = new Config(welcomeads);

        if (welcomeads.getConfig().getBoolean("inventory." + index + ".background.enable")) {
            this.background = welcomeads.getConfig().getString("inventory." + index + ".background.text",
                    welcomeads.getConfig().getString("background.text"));
            this.backgroundStay = welcomeads.getConfig().getInt("inventory." + index + ".background.stay",
                    welcomeads.getConfig().getInt("background.stay"));
            this.backgroundFadeout = welcomeads.getConfig().getInt("inventory." + index + ".background.fadeout",
                    welcomeads.getConfig().getInt("background.fadeout"));
        } else {
            this.background = welcomeads.getConfig().getString("background.text");
            this.backgroundStay = welcomeads.getConfig().getInt("background.stay");
            this.backgroundFadeout = welcomeads.getConfig().getInt("background.fadeout");
        }

        this.title = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player,
                "&f" + this.background + "&f" + welcomeads.getConfig().getString("inventory." + index + ".title")));
        this.itemsection = welcomeads.getConfig().getConfigurationSection("inventory." + this.index + ".items");
        this.holder = new WelcomeInventoryHolder(this);
        this.screenInventory = Bukkit.createInventory(this.holder, 54, this.title);

        preloadItems(player);
    }

    private void preloadItems(Player player) {
        if (itemsPreloaded || itemsection == null)
            return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String key : itemsection.getKeys(false)) {
            String headOwner = player.getName();
            String itemMaterial = itemsection.getString(key + ".material");
            int slot = itemsection.getInt(key + ".slot");
            slotToKeyMap.put(slot, key);

            List<String> commands = itemsection.getStringList(key + ".commands");
            if (!commands.isEmpty()) {
                commandMap.put(key, commands);
            }

            if (itemMaterial != null && itemMaterial.contains("PLAYER_HEAD") && itemMaterial.contains(":")) {
                String[] parts = itemMaterial.split(":");
                itemMaterial = parts[0];
                headOwner = parts[1];
            }

            if (itemMaterial != null && itemMaterial.contains("PLAYER_HEAD")) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                futures.add(future);

                HeadsUtil.getPlayerHead(headOwner, head -> {
                    if (head != null) {
                        ItemStack item = setupItemMeta(head, key, player,
                                itemsection.getString(key + ".name"),
                                itemsection.getInt(key + ".modeldata"),
                                itemsection.getStringList(key + ".lore"));
                        preloadedItems.put(slot, item);
                    }
                    future.complete(null);
                });
            } else {
                ItemStack item = createItemSync(key, player);
                preloadedItems.put(slot, item);
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        itemsPreloaded = true;
    }

    public boolean handleClick(int slot, Player player) {
        String key = slotToKeyMap.get(slot);
        if (key != null && commandMap.containsKey(key)) {
            List<String> commands = commandMap.get(key);
            if (!commands.isEmpty()) {
                CommandConverter.runStringListCommands(commands, player);
                return true;
            }
        }
        return false;
    }

    private ItemStack createItemSync(String key, Player player) {
        String itemName = itemsection.getString(key + ".name");
        String itemMaterial = itemsection.getString(key + ".material");

        if (itemMaterial == null || itemName == null)
            return null;

        Material material = Material.getMaterial(itemMaterial);
        if (material == null)
            return null;

        ItemStack item = new ItemStack(material);
        return setupItemMeta(item, key, player, itemName,
                itemsection.getInt(key + ".modeldata"),
                itemsection.getStringList(key + ".lore"));
    }

    @SuppressWarnings("deprecation")
    private ItemStack setupItemMeta(ItemStack item, String key, Player player,
            String itemName, int itemModelData, List<String> itemLore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.setDisplayName(PlaceholderAPI.setPlaceholders(player,
                ChatColor.translateAlternateColorCodes('&', itemName)));

        List<String> processedLore = new ArrayList<>(itemLore.size());
        for (String line : itemLore) {
            processedLore.add(PlaceholderAPI.setPlaceholders(player,
                    ChatColor.translateAlternateColorCodes('&', line)));
        }
        meta.setLore(processedLore);

        if (itemModelData >= 0) {
            meta.setCustomModelData(itemModelData);
        }

        List<String> flagList = itemsection.getStringList(key + ".flags");
        for (String flagkey : flagList) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagkey));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<String> enchantList = itemsection.getStringList(key + ".enchantments");
        for (String enchantkey : enchantList) {
            String[] eKey = enchantkey.split(":");
            if (eKey.length >= 2) {
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(eKey[0].toLowerCase()));
                if (ench != null) {
                    meta.addEnchant(ench, Integer.parseInt(eKey[1]), true);
                }
            }
        }

        if (item.getType().name().contains("LEATHER") || item.getType().name().contains("POTION")) {
            Color mixedColor = Color.fromRGB(255, 255, 255);
            List<String> dyeList = itemsection.getStringList(key + ".dyes");
            for (String dyekey : dyeList) {
                String[] dKey = dyekey.split(":");
                if (dKey.length >= 3) {
                    Color cKey = Color.fromRGB(
                            Integer.parseInt(dKey[0]),
                            Integer.parseInt(dKey[1]),
                            Integer.parseInt(dKey[2]));
                    mixedColor = mixedColor.mixColors(cKey);
                }
            }

            if (item.getType().name().contains("LEATHER")) {
                ((LeatherArmorMeta) meta).setColor(mixedColor);
            } else if (item.getType().name().contains("POTION")) {
                ((PotionMeta) meta).setColor(mixedColor);
            }
        }
        item.setItemMeta(meta);

        if (item.getType() == Material.PLAYER_HEAD && !item.hasItemMeta()) {
            SkullMeta skullmeta = (SkullMeta) item.getItemMeta();
            String material = itemsection.getString(key + ".material");
            if (material != null && material.contains(":")) {
                String headOwner = material.split(":")[1];
                skullmeta.setOwningPlayer(Bukkit.getOfflinePlayer(headOwner));
            }
            item.setItemMeta(skullmeta);
        }

        NBT.modify(item, nbt -> {
            nbt.setString("adsid", key);
            nbt.setBoolean("welcomeads", true);
        });
        return item;
    }

    public Inventory getScreenInventory(@Nullable Player player) {
        if (player == null) {
            player = Bukkit.getPlayer("Steve");
        }
        for (Map.Entry<Integer, ItemStack> entry : preloadedItems.entrySet()) {
            screenInventory.setItem(entry.getKey(), entry.getValue());
        }
        return screenInventory;
    }

    public void openTo(Player player) {
        if (itemsection == null) {
            player.sendMessage(config.loadLang("screen-config-none").replace("<index>", index));
            return;
        }

        if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            player.closeInventory();
        }

        String perm = welcomeads.getConfig().getString("inventory." + index + ".permission",
                "welcomeads.open." + index);

        if (player.hasPermission(perm)) {
            if (InventoryStorage.isHaveInventoryStorage(player)) {
                InventoryStorage storage = InventoryStorage.getInventoryStorage(player);
                storage.unloadInventoryStorage();
                InventoryStorage.removeInventoryStorage(storage);
            }

            InventoryStorage storage = new InventoryStorage(player);
            storage.loadInventoryStorage();
            InventoryStorage.addInventoryStorage(storage);
            player.openInventory(getScreenInventory(player));
        } else {
            player.sendMessage(config.loadLang("cmd-perm-none"));
        }
    }

    public WelcomeInventoryHolder getHolder() {
        return holder;
    }

    public String getIndex() {
        return index;
    }

    public String getBackground() {
        return background;
    }

    public Integer getBackgroundStay() {
        return backgroundStay;
    }

    public Integer getBackgroundFadeout() {
        return backgroundFadeout;
    }

    public String getTitle() {
        return title;
    }

    public ConfigurationSection getItemSection() {
        return itemsection;
    }
}