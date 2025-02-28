package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.bukkit.profile.PlayerProfile;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;

public final class Screen {

    SkinsRestorer skinsRestorerAPI;
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

    public Screen(String index, Player player) {
        this.index = index;
        this.config = new Config(welcomeads);
        if (welcomeads.getConfig().getBoolean("inventory." + index + ".background.enable") == true) {
            String bg = welcomeads.getConfig().getString("inventory." + index + ".background.text");
            if (bg == null) {
                this.background = welcomeads.getConfig().getString("background.text");
            } else {
                this.background = bg;
            }
            Integer bgstay = welcomeads.getConfig().getInt("inventory." + index + ".background.stay");
            if (bgstay >= 0) {
                this.backgroundStay = bgstay;
            } else {
                this.backgroundStay = welcomeads.getConfig().getInt("background.stay");
            }
            Integer bgfadeout = welcomeads.getConfig().getInt("inventory." + index + ".background.fadeout");
            if (bgfadeout >= 0) {
                this.backgroundFadeout = bgfadeout;
            } else {
                this.backgroundFadeout = welcomeads.getConfig().getInt("background.fadeout");
            }
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
    }

    public WelcomeInventoryHolder getHolder() {
        return this.holder;
    }

    public String getIndex() {
        return this.index;
    }

    public String getBackground() {
        return this.background;
    }

    public Integer getBackgroundStay() {
        return this.backgroundStay;
    }

    public Integer getBackgroundFadeout() {
        return this.backgroundFadeout;
    }

    public String getTitle() {
        return this.title;
    }

    public Inventory getScreenInventory(@Nullable Player player) {
        if (player == null) {
            player = Bukkit.getPlayer("Steve");
        }
        for (String key : this.itemsection.getKeys(false)) {
            this.screenInventory.setItem(this.itemsection.getInt(key + ".slot"), getItem(key, player));
        }
        return this.screenInventory;
    }

    public ConfigurationSection getItemSection() {
        return this.itemsection;
    }

    public ItemStack getItem(String key, Player player) {
        String headOwner = player.getName();
        String itemName = this.itemsection.getString(key + ".name");
        String itemMaterial = this.itemsection.getString(key + ".material");
        int itemModelData = this.itemsection.getInt(key + ".modeldata");
        List<String> itemLore = this.itemsection.getStringList(key + ".lore");
        if (itemMaterial == null || itemName == null) {
            return null;
        }
        if (itemLore.isEmpty()) {
            itemLore = new ArrayList<>();
        }
        if (itemMaterial.contains(":")) {
            String material = itemMaterial.split(":")[0];
            headOwner = itemMaterial.split(":")[1];
            itemMaterial = material;
        }

        ItemStack item = new ItemStack(Material.getMaterial(itemMaterial));
        if (Material.getMaterial(itemMaterial) == Material.PLAYER_HEAD) {
            try {
                this.skinsRestorerAPI = SkinsRestorerProvider.get();
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                SkinProperty skinProperty = this.skinsRestorerAPI.getSkinStorage().getPlayerSkin(headOwner, false)
                        .orElse(null).getSkinProperty();
                UUID skinUUID = this.skinsRestorerAPI.getSkinStorage().getPlayerSkin(headOwner, false).orElse(null)
                        .getUniqueId();
                String textures = skinProperty.getValue();
                PlayerProfile playerProfile = Bukkit.getServer().createPlayerProfile(skinUUID, headOwner);
                meta.setOwnerProfile(playerProfile);
                item.setItemMeta(meta);
                NBT.modifyComponents(item, nbt -> {
                    ReadWriteNBT profileNbt = nbt.getOrCreateCompound("minecraft:profile");
                    profileNbt.setUUID("id", skinUUID);
                    ReadWriteNBT propertiesNbt = profileNbt.getCompoundList("properties").addCompound();
                    propertiesNbt.setString("name", "textures");
                    propertiesNbt.setString("value", textures);
                });
                ItemMeta skullmeta = item.getItemMeta();
                if (skullmeta != null) {
                    skullmeta.setCustomModelData(itemModelData);
                    skullmeta.setDisplayName(PlaceholderAPI.setPlaceholders(player,
                            ChatColor.translateAlternateColorCodes('&', itemName)));
                    for (int i = 0; i < itemLore.size(); i++) {
                        itemLore.set(i, PlaceholderAPI.setPlaceholders(player,
                                ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
                    }
                    skullmeta.setLore(itemLore);
                    item.setItemMeta(skullmeta);
                }
            } catch (DataRequestException ex) {
            }
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (itemModelData >= 0) {
                    meta.setCustomModelData(itemModelData);
                    item.setItemMeta(meta);
                    meta.setDisplayName(PlaceholderAPI.setPlaceholders(player,
                            ChatColor.translateAlternateColorCodes('&', itemName)));
                    for (int i = 0; i < itemLore.size(); i++) {
                        itemLore.set(i, PlaceholderAPI.setPlaceholders(player,
                                ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
                    }
                    meta.setLore(itemLore);
                    item.setItemMeta(meta);
                }
            }
        }

        // Adding Flags to the item
        // Adding Enchantments to the item
        // Adding DyeColor to the item
        ItemMeta meta = item.getItemMeta();
        List<String> flagList = this.itemsection.getStringList(key + ".flags");
        List<String> enchantList = this.itemsection.getStringList(key + ".enchantments");
        List<String> dyelist = this.itemsection.getStringList(key + ".dyes");

        if (meta != null) {
            for (String flagkey : flagList) {
                ItemFlag flag = ItemFlag.valueOf(flagkey);
                if (flag != null){
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flagkey));
                }
            }
            for (String enchantkey : enchantList) {
                String[] eKey = enchantkey.split(":");
                if (eKey[0] != null && eKey[1] == null) {
                    eKey[1] = "1";
                }

                if (eKey[0] != null && eKey[1] != null) {
                    Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(eKey[0].toLowerCase()));
                    if (ench != null){
                        meta.addEnchant(ench, Integer.parseInt(eKey[1]), true);
                    }
                }
            }

            if (item.getType().name().contains("LEATHER") || item.getType().name().contains("POTION")) {
                Color mixedColor = Color.fromRGB(255,255,255);
                for (String dyekey : dyelist) {
                    String[] dKey = dyekey.split(":");
                    if (dKey[0] != null && dKey[1] != null && dKey[2] != null) {
                        Color cKey = Color.fromRGB(Integer.parseInt(dKey[0]), Integer.parseInt(dKey[1]), Integer.parseInt(dKey[2]));
                        mixedColor = mixedColor.mixColors(cKey);
                    }
                }
                if (item.getType().name().contains("LEATHER")) {
                    ((LeatherArmorMeta) meta).setColor(mixedColor);
                }
                else if (item.getType().name().contains("POTION")) {
                    ((PotionMeta) meta).setColor(mixedColor);
                }
            }

            item.setItemMeta(meta);
        }

        // Adding Custom NBT to the item -> to define the custom items from the plugin
        NBT.modify(item, nbt -> {
            nbt.setString("adsid", index);
            nbt.setBoolean("welcomeads", true);
        });
        return item;
    }

    public void openTo(Player player) {
        if (this.itemsection == null) {
            player.sendMessage(this.config.loadLang("screen-config-none").replace("<index>", this.index));
        } else {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
                player.closeInventory();
            }
            String perm = welcomeads.getConfig().getString("inventory." + this.index + "permission");
            if (perm == null) {
                perm = "welcomeads.open." + this.index;
            }
            if (player.hasPermission(perm)) {

                if (WelcomeAds.isHaveInventoryStorage(player)) {
                    InventoryStorage storage = WelcomeAds.getInventoryStorage(player);
                    storage.unloadInventoryStorage();
                    WelcomeAds.removeInventoryStorage(storage);
                }
                InventoryStorage storage = new InventoryStorage(player);
                storage.loadInventoryStorage();
                WelcomeAds.addInventoryStorage(storage);
                player.openInventory(getScreenInventory(player));
            } else {
                player.sendMessage(this.config.loadLang("cmd-perm-none"));
            }
        }
    }
}
