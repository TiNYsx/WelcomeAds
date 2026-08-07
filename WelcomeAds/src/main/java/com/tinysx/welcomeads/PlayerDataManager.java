package com.tinysx.welcomeads;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class PlayerDataManager {

    private static final Map<UUID, PlayerSessionData> DATA_MAP = new ConcurrentHashMap<>();

    private PlayerDataManager() {
    }

    public static class PlayerSessionData {
        private boolean seenStatus = false;
        private boolean joinStatus = false;
        private BukkitTask activeTransitionTask = null;

        public boolean isSeen() {
            return seenStatus;
        }

        public void setSeen(boolean seen) {
            this.seenStatus = seen;
        }

        public boolean isJoined() {
            return joinStatus;
        }

        public void setJoined(boolean joined) {
            this.joinStatus = joined;
        }

        public void setTransitionTask(BukkitTask task) {
            cancelTransitionTask();
            this.activeTransitionTask = task;
        }

        public void cancelTransitionTask() {
            if (this.activeTransitionTask != null && !this.activeTransitionTask.isCancelled()) {
                this.activeTransitionTask.cancel();
            }
            this.activeTransitionTask = null;
        }
    }

    public static PlayerSessionData getOrCreate(UUID uuid) {
        return DATA_MAP.computeIfAbsent(uuid, k -> new PlayerSessionData());
    }

    public static PlayerSessionData get(Player player) {
        if (player == null) {
            return null;
        }
        return DATA_MAP.get(player.getUniqueId());
    }

    public static boolean isHavePlayerData(Player player) {
        if (player == null) {
            return false;
        }
        return DATA_MAP.containsKey(player.getUniqueId());
    }

    public static void handleDisconnect(UUID uuid) {
        PlayerSessionData data = DATA_MAP.get(uuid);
        if (data != null) {
            data.cancelTransitionTask();
            data.setJoined(false);
        }
    }

    public static void remove(UUID uuid) {
        PlayerSessionData data = DATA_MAP.remove(uuid);
        if (data != null) {
            data.cancelTransitionTask();
        }
    }

    public static void clearAll() {
        for (PlayerSessionData data : DATA_MAP.values()) {
            data.cancelTransitionTask();
        }
        DATA_MAP.clear();
    }
}
