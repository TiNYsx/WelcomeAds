package com.tinysx.welcomeads.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.tinysx.welcomeads.Config;
import com.tinysx.welcomeads.InventoryStorage;
import com.tinysx.welcomeads.PlayerDataStorage;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeAds;

public class PlayerListener implements Listener {

    private final WelcomeAds welcomeads = WelcomeAds.getPlugin(WelcomeAds.class);
    Config config = this.welcomeads.config;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = (Player) event.getPlayer();
        if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
            PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
            storage.getStatus().setJoinStatus(false);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = (Player) event.getPlayer();
        if (InventoryStorage.isHaveInventoryStorage(player)) {
            InventoryStorage istorage = InventoryStorage.getInventoryStorage(player);
            istorage.unloadInventoryStorage();
            InventoryStorage.removeInventoryStorage(istorage);
        }
        String loadtype = welcomeads.getConfig().getString("loadtype");
        if (loadtype != null && (loadtype.equalsIgnoreCase("onjoin") || loadtype.equalsIgnoreCase("both"))) {
            String page = welcomeads.getConfig().getString("joinpage");
            if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                if (welcomeads.getConfig().getBoolean("persession") == true) {
                    PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                    storage.getStatus().setJoinStatus(true);
                    if (storage.getSeenStatus() == true) {
                        return;
                    } else {
                        storage.setSeenStatus(true);
                    }
                }
                new Screen(page, 0, player).openTo(player);
            } else {
                PlayerDataStorage storage = new PlayerDataStorage(player);
                storage.setSeenStatus(true);
                storage.getStatus().setJoinStatus(true);
                PlayerDataStorage.addPlayerDataStorage(storage);
                new Screen(page, 0, player).openTo(player);
            }
        } else if (loadtype != null && (loadtype.equalsIgnoreCase("onresourcepack"))) {
            String page = welcomeads.getConfig().getString("joinpage");
            if (config.loadInventory().getBoolean("inventory." + page + ".enable") != false) {
                if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                    PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                    if (storage.getStatus().getJoinStatus() == true) {
                    } else {
                        storage.getStatus().setJoinStatus(true);
                    }
                } else {
                    PlayerDataStorage storage = new PlayerDataStorage(player);
                    storage.getStatus().setJoinStatus(true);
                    PlayerDataStorage.addPlayerDataStorage(storage);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerResourcepackLoaded(PlayerResourcePackStatusEvent event) {
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            Player player = (Player) event.getPlayer();
            String loadtype = welcomeads.getConfig().getString("loadtype");
            if (loadtype != null && (loadtype.equalsIgnoreCase("onresourcepack") || loadtype.equalsIgnoreCase("both"))) {
                String page = welcomeads.getConfig().getString("joinpage");
                if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                    PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                    if (welcomeads.getConfig().getBoolean("persession") == true) {
                        if (storage.getSeenStatus() == true) {
                            return;
                        }
                    }
                    if (storage.getStatus().getJoinStatus() != true) {
                        new BukkitRunnable() {
                            Integer count = 0;

                            @Override
                            public void run() {
                                if (storage.getStatus().getJoinStatus() == true) {
                                    storage.setSeenStatus(true);
                                    new Screen(page, 0, player).openTo(player);
                                    cancel();
                                } else if (count < 20) {
                                    count++;
                                } else {
                                    cancel();
                                }
                            }
                        }.runTaskTimer(this.welcomeads, 10L, 10L);
                    } else {
                        storage.setSeenStatus(true);
                        new Screen(page, 0, player).openTo(player);
                    }
                } else {
                    PlayerDataStorage storage = new PlayerDataStorage(player);
                    PlayerDataStorage.addPlayerDataStorage(storage);
                    new BukkitRunnable() {
                        Integer count = 0;

                        @Override
                        public void run() {
                            if (storage.getStatus().getJoinStatus() == true) {
                                storage.setSeenStatus(true);
                                new Screen(page, 0, player).openTo(player);
                                cancel();
                            } else if (count < 20) {
                                count++;
                            } else {
                                cancel();
                            }
                        }
                    }.runTaskTimer(this.welcomeads, 10L, 10L);
                }
            }
        }
    }
}
