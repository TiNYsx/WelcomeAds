package com.tinysx.welcomeads;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;

public final class Config {
    private File lang;
    private Plugin plugin;
    private YamlConfiguration langConfig; // Cache the configuration
    
    public Config(Plugin plugin) {
        this.plugin = plugin;
        // Initialize the file and config upon creation
        configLoad();
    }
    
    public String loadLang(String path) {
        // Ensure the lang file is loaded
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
    
    public void configLoad() {
        plugin.saveDefaultConfig();
        
        // Check if lang.yml exists before trying to save it
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        
        reload();
    }
    
    public void reload() {
        this.lang = new File(plugin.getDataFolder(), "lang.yml");
        
        // Check if file exists before loading
        if (this.lang.exists()) {
            this.langConfig = YamlConfiguration.loadConfiguration(this.lang);
        } else {
            plugin.getLogger().severe("Language file does not exist: " + this.lang.getAbsolutePath());
            this.langConfig = new YamlConfiguration(); // Create empty config to avoid NullPointerException
        }
    }
}