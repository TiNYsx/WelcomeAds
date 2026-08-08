package com.tinysx.welcomeads;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
        saveDefaultPacks();
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

    public void saveDefaultPacks() {
        File packsFolder = new File(plugin.getDataFolder(), "packs");
        if (packsFolder.exists() && packsFolder.list() != null && packsFolder.list().length > 0) {
            return;
        }

        try {
            URL src = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (src != null) {
                File codeSourceFile = new File(src.toURI());
                if (codeSourceFile.isFile()) {
                    try (JarFile jar = new JarFile(codeSourceFile)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith("packs/") && !entry.isDirectory()) {
                                plugin.saveResource(name, false);
                            }
                        }
                    }
                } else if (codeSourceFile.isDirectory()) {
                    File devPacks = new File(codeSourceFile, "packs");
                    if (devPacks.exists()) {
                        copyDirectory(devPacks, packsFolder);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract default packs directory: " + e.getMessage());
        }
    }

    private void copyDirectory(File source, File target) {
        if (!source.exists()) {
            return;
        }
        File[] files = source.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            File dest = new File(target, file.getName());
            if (file.isDirectory()) {
                dest.mkdirs();
                copyDirectory(file, dest);
            } else if (!dest.exists()) {
                try {
                    dest.getParentFile().mkdirs();
                    Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
