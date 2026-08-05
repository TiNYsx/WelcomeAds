package com.tinysx.welcomeads;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SafeInventoryManager {

    private static final Map<UUID, ItemStack[]> INVENTORY_CACHE = new ConcurrentHashMap<>();

    private SafeInventoryManager() {
    }

    public static boolean isHidingEnabled() {
        return WelcomeAds.getInstance().getConfig().getBoolean("hide-player-inventory", true);
    }

    public static boolean hasHiddenInventory(Player player) {
        if (player == null) {
            return false;
        }
        return INVENTORY_CACHE.containsKey(player.getUniqueId());
    }

    public static void hidePlayerInventory(Player player) {
        if (player == null || !isHidingEnabled()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        // If already hidden (e.g. transitioning between pages), do not overwrite with empty inventory
        if (INVENTORY_CACHE.containsKey(uuid)) {
            return;
        }

        // 1. Capture full contents (storage, armor, offhand)
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] clonedContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                clonedContents[i] = contents[i].clone();
            }
        }

        INVENTORY_CACHE.put(uuid, clonedContents);

        // 2. Write emergency disk backup in case of hard server power loss/crash
        saveEmergencyBackup(uuid, clonedContents);

        // 3. Clear player inventory so the Welcome Ads screen is 100% clean
        player.getInventory().clear();
        player.updateInventory();
    }

    public static void restorePlayerInventory(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        ItemStack[] savedContents = INVENTORY_CACHE.remove(uuid);

        if (savedContents != null) {
            player.getInventory().setContents(savedContents);
            player.updateInventory();
        }

        // Delete emergency backup once restored
        deleteEmergencyBackup(uuid);
    }

    public static void restoreAll() {
        for (Map.Entry<UUID, ItemStack[]> entry : INVENTORY_CACHE.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.getInventory().setContents(entry.getValue());
                player.updateInventory();
            }
            deleteEmergencyBackup(entry.getKey());
        }
        INVENTORY_CACHE.clear();
    }

    public static void checkAndRestoreCrashBackup(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        File backupFile = getBackupFile(uuid);
        if (backupFile.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(backupFile);
                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) yaml.get("contents");
                if (items != null && !items.isEmpty()) {
                    ItemStack[] contents = items.toArray(new ItemStack[0]);
                    player.getInventory().setContents(contents);
                    player.updateInventory();
                    WelcomeAds.getInstance().getLogger().info("Restored emergency inventory backup for player " + player.getName());
                }
            } catch (Exception e) {
                WelcomeAds.getInstance().getLogger().warning("Failed to restore emergency inventory backup for " + player.getName() + ": " + e.getMessage());
            } finally {
                deleteEmergencyBackup(uuid);
            }
        }
    }

    private static void saveEmergencyBackup(UUID uuid, ItemStack[] contents) {
        try {
            File backupFile = getBackupFile(uuid);
            File parent = backupFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("contents", contents);
            yaml.save(backupFile);
        } catch (Exception ignored) {
        }
    }

    private static void deleteEmergencyBackup(UUID uuid) {
        try {
            File backupFile = getBackupFile(uuid);
            if (backupFile.exists()) {
                backupFile.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static File getBackupFile(UUID uuid) {
        return new File(WelcomeAds.getInstance().getDataFolder(), "backups/" + uuid.toString() + ".yml");
    }
}
