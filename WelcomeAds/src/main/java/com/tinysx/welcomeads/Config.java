package com.tinysx.welcomeads;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.tinysx.welcomeads.utils.ColorUtil;

public final class Config {

    private final Plugin plugin;
    private final Map<String, String> rawLangCache = new HashMap<>();

    private File langFile;
    private YamlConfiguration langConfig;

    private File inventoryFile;
    private YamlConfiguration inventoryConfig;

    private File containerFile;
    private YamlConfiguration containerConfig;

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
        return loadLang(path, null);
    }

    public String loadLang(String path, Player player) {
        String raw = rawLangCache.get(path);
        if (raw == null) {
            return "§7[§c!§7] §cMissing language key: " + path;
        }
        return ColorUtil.format(player, raw);
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
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!this.langFile.exists()) {
            this.plugin.saveResource("lang.yml", false);
        }
        this.langConfig = YamlConfiguration.loadConfiguration(this.langFile);

        rawLangCache.clear();
        for (String key : this.langConfig.getKeys(false)) {
            String val = this.langConfig.getString(key);
            if (val != null) {
                rawLangCache.put(key, val);
            }
        }
    }

    public void reloadInventory() {
        this.inventoryFile = new File(plugin.getDataFolder(), "inventory.yml");
        if (!this.inventoryFile.exists()) {
            this.plugin.saveResource("inventory.yml", false);
        }
        this.inventoryConfig = YamlConfiguration.loadConfiguration(this.inventoryFile);
    }

    public void reloadContainer() {
        this.containerFile = new File(plugin.getDataFolder(), "container.yml");
        if (!this.containerFile.exists()) {
            this.plugin.saveResource("container.yml", false);
        }
        this.containerConfig = YamlConfiguration.loadConfiguration(this.containerFile);
    }
}
