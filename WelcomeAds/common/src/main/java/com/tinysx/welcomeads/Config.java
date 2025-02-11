package com.tinysx.welcomeads;

import java.io.File;
import java.util.Optional;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;

public final class Config {
    File lang;
    Plugin plugin;

    public Config(Plugin plugin) {
        this.plugin = plugin;
    }

    public String loadLang(String path) {
        return ChatColor.translateAlternateColorCodes('&', Optional.of((YamlConfiguration.loadConfiguration(this.lang)).getString(path, "Config not found " + path)).orElse("§7[§c!§7] §cThe lang config for " + path + "is not set, please config this in lang.yml file."));
    }

    public FileConfiguration loadConfig() {
        return plugin.getConfig();
    }

    public void configLoad() {
        plugin.saveDefaultConfig();
        plugin.saveResource("lang.yml", false);
        reload();
    }

    public void reload() {
        this.lang = new File(plugin.getDataFolder(), "lang.yml");
    }

}