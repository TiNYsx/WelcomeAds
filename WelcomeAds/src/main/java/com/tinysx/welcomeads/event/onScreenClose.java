package com.tinysx.welcomeads.event;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeAds;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.utils.CommandConverter;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public final class onScreenClose implements Listener {

    private final WelcomeAds welcomeads = WelcomeAds.getPlugin(WelcomeAds.class);
    public Player player;
    public Screen screen;
    public String index;
    private final FileConfiguration config;

    public onScreenClose(InventoryCloseEvent event, WelcomeInventoryHolder holder) {

        this.player = (Player) event.getPlayer();
        this.screen = holder.getScreen();
        this.config = welcomeads.getConfig();
        this.index = this.screen.getIndex();

        
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&',
                    PlaceholderAPI.setPlaceholders(player,
                            screen.getBackground() != null ? screen.getBackground() : "")),
            "", 0, 20, 20);

        new BukkitRunnable() {
            @Override
            public void run(){
                // checking if the page is forcing to stay or not
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder == false) {
                    // esc closing & force: true
                    if (config.getBoolean("inventory." + index + ".force") == true) {
                        player.closeInventory();
                        new Screen(index, player).openTo(player);
                    }
                    else {
                        // sending background fade title to player
                        player.sendTitle(
                            ChatColor.translateAlternateColorCodes('&',
                                    PlaceholderAPI.setPlaceholders(player,
                                            screen.getBackground() != null ? screen.getBackground() : "")),
                            "", 0, screen.getBackgroundStay(), screen.getBackgroundFadeout());

                        // getting, loading and remove the player's item storage
                        if (WelcomeAds.isHaveInventoryStorage(player)) {
                            WelcomeAds.getInventoryStorage(player).unloadInventoryStorage();
                            WelcomeAds.removeInventoryStorage(WelcomeAds.getInventoryStorage(player));
                        }

                        // running event command
                        List<String> cmds = config.getStringList("inventory." + index + ".events.onInventoryClose.commands");
                        if (!cmds.isEmpty()) {
                            CommandConverter.runStringListCommands(cmds, player);
                        }
                    }
                }
            }
        }.runTaskLater(welcomeads, 1L);
    }

    public Player getPlayer() {
        return this.player;
    }

    public Screen getScreen() {
        return this.screen;
    }
}