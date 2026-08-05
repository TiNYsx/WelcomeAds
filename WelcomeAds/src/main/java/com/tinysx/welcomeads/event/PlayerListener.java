package com.tinysx.welcomeads.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.tinysx.welcomeads.PlayerDataManager;
import com.tinysx.welcomeads.SafeInventoryManager;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeAds;

public class PlayerListener implements Listener {

    private final WelcomeAds welcomeads = WelcomeAds.getInstance();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Restore inventory immediately before player leaves and data is saved to world
        SafeInventoryManager.restorePlayerInventory(event.getPlayer());
        PlayerDataManager.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKick(PlayerKickEvent event) {
        SafeInventoryManager.restorePlayerInventory(event.getPlayer());
        PlayerDataManager.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Crash recovery: Check if an emergency backup file exists from a previous crash/power outage
        SafeInventoryManager.checkAndRestoreCrashBackup(player);

        PlayerDataManager.PlayerSessionData session = PlayerDataManager.getOrCreate(player.getUniqueId());
        session.setJoined(true);

        String loadType = welcomeads.getConfig().getString("loadtype", "onjoin");
        boolean perSession = welcomeads.getConfig().getBoolean("persession", true);
        String page = welcomeads.getConfig().getString("joinpage", "welcome-1");

        if (loadType.equalsIgnoreCase("onjoin") || loadType.equalsIgnoreCase("both")) {
            if (perSession && session.isSeen()) {
                return;
            }
            session.setSeen(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        new Screen(page, 0, player).openTo(player);
                    }
                }
            }.runTaskLater(welcomeads, 5L);
        }
    }

    @EventHandler
    public void onPlayerResourcepackLoaded(PlayerResourcePackStatusEvent event) {
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            Player player = event.getPlayer();
            String loadType = welcomeads.getConfig().getString("loadtype", "onjoin");
            boolean perSession = welcomeads.getConfig().getBoolean("persession", true);
            String page = welcomeads.getConfig().getString("joinpage", "welcome-1");

            if (loadType.equalsIgnoreCase("onresourcepack") || loadType.equalsIgnoreCase("both")) {
                PlayerDataManager.PlayerSessionData session = PlayerDataManager.getOrCreate(player.getUniqueId());
                if (perSession && session.isSeen()) {
                    return;
                }
                session.setSeen(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            new Screen(page, 0, player).openTo(player);
                        }
                    }
                }.runTaskLater(welcomeads, 5L);
            }
        }
    }
}
