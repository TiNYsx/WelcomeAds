package com.tinysx.welcomeads;

import java.io.File;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class Config {
    File lang;
    Plugin plugin;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public String loadLang(String path) {
        return ChatColor.translateAlternateColorCodes('&', Optional.of((YamlConfiguration.loadConfiguration(this.lang)).getString(path, "Config not found " + path)).orElse("§7[§c!§7] §cThe lang config for " + path + "is not set, please config this in lang.yml file."));
    }

    public void reload() {
        this.lang = new File(plugin.getDataFolder(), "lang.yml");
    }

}