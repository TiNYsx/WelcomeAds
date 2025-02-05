package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;

public final class Screen {

    SkinsRestorer skinsRestorerAPI;
    private final Inventory screenInventory;
    private final String index;
    private final String title;
    private final String background;
    private final ConfigurationSection itemsection;

    public Screen(String index, Player player, Plugin plugin) {
        this.index = index;
        this.background = plugin.getConfig().getString("background.text");
        this.title = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, "&f" + this.background + "&f" + plugin.getConfig().getString("inventory." + index + ".title")));
        this.itemsection = plugin.getConfig().getConfigurationSection("inventory." + this.index + ".items");
        this.screenInventory = Bukkit.createInventory(new WelcomeInventoryHolder(this.index), 54, this.title);
    }

    public String getIndex() {
        return this.index;
    }

    public String getBackground() {
        return this.background;
    }

    public String getTitle() {
        return this.title;
    }

    public Inventory getScreenInventory(Player player) {

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
                SkinProperty skinProperty = this.skinsRestorerAPI.getSkinStorage().getPlayerSkin(headOwner, false).orElse(null).getSkinProperty();
                UUID skinUUID = this.skinsRestorerAPI.getSkinStorage().getPlayerSkin(headOwner, false).orElse(null).getUniqueId();
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
                    skullmeta.setDisplayName(PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemName)));
                    for (int i = 0; i < itemLore.size(); i++) {
                        itemLore.set(i, PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
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
                    meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemName)));
                    for (int i = 0; i < itemLore.size(); i++) {
                        itemLore.set(i, PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
                    }
                    meta.setLore(itemLore);
                    item.setItemMeta(meta);
                }
            }
        }
        NBT.modify(item, nbt -> {
            nbt.setString("adsid", index);
            nbt.setBoolean("welcomeads", true);
        });
        return item;
    }

    public void openTo(Player player) {
        if (this.itemsection == null) {
            player.sendMessage("§7[§e!§7] §fThe config for the index §e" + index + "§f is empty, please read the document.");
        } 
        else {
            // todo : load player's inventory to InventoryStorage
            InventoryStorage storage = InventoryStorage.getInventoryStorage(player);
            
            if (storage == null) {
                InventoryStorage.createInventoryStorage(player);
                storage = InventoryStorage.getInventoryStorage(player);
            }
            storage.loadInventoryStorage(player);

            player.openInventory(getScreenInventory(player));   
        }
    }
}
