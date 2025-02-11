package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
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
    private final Plugin plugin;
    

    public Screen(String index, Player player, Plugin plugin) {
        this.index = index;
        this.plugin = plugin;
        this.config = new Config(this.plugin);
        if (this.plugin.getConfig().getBoolean("inventory." + index + ".background.enable") == true) {
            String bg = this.plugin.getConfig().getString("inventory." + index + ".background.text");
            if (bg == null) {this.background = this.plugin.getConfig().getString("background.text");}
            else {this.background = bg;}
            Integer bgstay = this.plugin.getConfig().getInt("inventory." + index + ".background.stay");
            if (bgstay >= 0) {this.backgroundStay = bgstay;} 
            else {this.backgroundStay = this.plugin.getConfig().getInt("background.stay");}
            Integer bgfadeout = this.plugin.getConfig().getInt("inventory." + index + ".background.fadeout");
            if (bgfadeout >= 0) {this.backgroundFadeout = bgfadeout;} 
            else {this.backgroundFadeout = this.plugin.getConfig().getInt("background.fadeout");}
        }
        else {
            this.background = this.plugin.getConfig().getString("background.text");
            this.backgroundStay = this.plugin.getConfig().getInt("background.stay");
            this.backgroundFadeout = this.plugin.getConfig().getInt("background.fadeout");
        }
        this.title = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, "&f" + this.background + "&f" + this.plugin.getConfig().getString("inventory." + index + ".title")));
        this.itemsection = this.plugin.getConfig().getConfigurationSection("inventory." + this.index + ".items");
        this.holder = new WelcomeInventoryHolder(this);
        this.screenInventory = Bukkit.createInventory(this.holder, 54, this.title);
    }

    public WelcomeInventoryHolder getHolder(){
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
        if (itemMaterial == null || itemName == null) {return null;}
        if (itemLore.isEmpty()) {itemLore = new ArrayList<>();}
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
            player.sendMessage(this.config.loadLang("screen-config-none").replace("<index>", this.index));
        } 
        else {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder){
                player.closeInventory();
            }
            String perm = this.plugin.getConfig().getString("inventory." + this.index + "permission");
            if (perm == null) {perm = "welcomeads.open."+this.index;}
            if (player.hasPermission(perm)) {
                InventoryStorage storage = InventoryStorage.getInventoryStorage(player);
                if (storage == null) {
                    InventoryStorage.createInventoryStorage(player);
                    storage = InventoryStorage.getInventoryStorage(player);
                }
                storage.loadInventoryStorage(player);
                player.openInventory(getScreenInventory(player));   
            }
            else {
                player.sendMessage(this.config.loadLang("cmd-perm-none"));
            }
        }
    }
}
