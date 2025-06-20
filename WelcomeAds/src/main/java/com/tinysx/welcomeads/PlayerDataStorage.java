package com.tinysx.welcomeads;

import org.bukkit.entity.Player;

public class PlayerDataStorage {

    private boolean seenStatus = false;
    private final Status status = new Status();
    private final Player player;

    public class Status {

        private boolean joinStatus = false;

        public boolean getJoinStatus() {
            return joinStatus;
        }

        public void setJoinStatus(boolean status) {
            this.joinStatus = status;
        }
    }

    public PlayerDataStorage(Player player) {
        this.player = player;
    }

    public static void addPlayerDataStorage(PlayerDataStorage storage) {
        WelcomeAds.playerDataStorages.add(storage);
    }

    public static void removePlayerDataStorage(PlayerDataStorage storage) {
        WelcomeAds.playerDataStorages.remove(storage);
    }

    public static PlayerDataStorage getPlayerDataStorage(Player player) {
        for (PlayerDataStorage storage : WelcomeAds.playerDataStorages) {
            if (storage.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                return storage;
            }
        }
        return null;
    }

    public static boolean isHavePlayerDataStorage(Player player) {
        return getPlayerDataStorage(player) != null;
    }

    public void setSeenStatus(boolean status) {
        this.seenStatus = status;
    }

    public boolean getSeenStatus() {
        return seenStatus;
    }

    public Player getPlayer() {
        return player;
    }

    public Status getStatus() {
        return status;
    }
}
