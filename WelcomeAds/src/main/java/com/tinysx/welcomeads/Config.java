package com.tinysx.welcomeads;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;

public final class Config {

    private File lang;
    private YamlConfiguration langConfig;
    private File inventory;
    private YamlConfiguration inventoryConfig;
    private File container;
    private YamlConfiguration containerConfig;
    private Plugin plugin;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.plugin.reloadConfig();
        reloadLang();
        reloadInventory();
        reloadContainer();
    }

    public String loadLang(String path) {
        if (langConfig == null) {
            reload();
        }
        String value = langConfig.getString(path, "Config not found " + path);
        if (value == null) {
            return "§7[§c!§7] §cThe lang config for " + path + " is not set, please config this in lang.yml file.";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public FileConfiguration loadConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration loadInventory() {
        if (inventoryConfig == null) {
            reloadInventory();
        }
        return inventoryConfig;
    }

    public FileConfiguration loadContainer() {
        if (containerConfig == null) {
            reloadContainer();
        }
        return containerConfig;
    }

    public void reloadLang() {
        this.lang = new File(plugin.getDataFolder(), "lang.yml");
        if (this.lang.exists()) {
            this.langConfig = YamlConfiguration.loadConfiguration(this.lang);
        } else {
            this.plugin.saveResource("lang.yml", false);
            this.langConfig = YamlConfiguration.loadConfiguration(this.lang);
            if (this.langConfig == null) {
                plugin.getLogger().log(Level.SEVERE, "Language file does not exist: {0}", this.lang.getAbsolutePath());
            }
        }
    }

    public void reloadInventory() {
        this.inventory = new File(plugin.getDataFolder(), "inventory.yml");
        if (this.inventory.exists()) {
            this.inventoryConfig = YamlConfiguration.loadConfiguration(this.inventory);
        } else {
            this.plugin.saveResource("inventory.yml", false);
            this.inventoryConfig = YamlConfiguration.loadConfiguration(this.inventory);
            if (this.inventoryConfig == null) {
                plugin.getLogger().log(Level.SEVERE, "Inventory file does not exist: {0}", this.inventory.getAbsolutePath());
            }
        }
    }

    public void reloadContainer() {
        this.container = new File(plugin.getDataFolder(), "container.yml");
        if (this.container.exists()) {
            this.containerConfig = YamlConfiguration.loadConfiguration(this.container);
        } else {
            this.plugin.saveResource("container.yml", false);
            this.containerConfig = YamlConfiguration.loadConfiguration(this.container);
            if (this.containerConfig == null) {
                plugin.getLogger().log(Level.SEVERE, "Container file does not exist: {0}", this.container.getAbsolutePath());
            }
        }
    }
}
