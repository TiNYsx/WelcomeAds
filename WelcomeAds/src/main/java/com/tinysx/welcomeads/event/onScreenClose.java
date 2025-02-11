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

        new BukkitRunnable() {
            @Override
            public void run(){
                // checking if the page is forcing to stay or not
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder == false) {
                    // esc closing & force: true
                    if (config.getBoolean("inventory." + index + ".force") == true) {
                        new Screen(index, player).openTo(player);
                    }
                }
            }
        }.runTaskLater(welcomeads, 2L);

        // sending background fade title to player
        this.player.sendTitle(
                ChatColor.translateAlternateColorCodes('&',
                        PlaceholderAPI.setPlaceholders(this.player,
                                this.screen.getBackground() != null ? this.screen.getBackground() : "")),
                "", 0, this.screen.getBackgroundStay(), this.screen.getBackgroundFadeout());

        // getting, loading and remove the player's item storage
        if (WelcomeAds.isHaveInventoryStorage(this.player)) {
            WelcomeAds.getInventoryStorage(this.player).unloadInventoryStorage();
            WelcomeAds.removeInventoryStorage(WelcomeAds.getInventoryStorage(this.player));
        }

        // running event command
        List<String> cmds = this.config.getStringList("inventory." + this.index + ".events.onInventoryClose.commands");
        if (!cmds.isEmpty()) {
            CommandConverter.runStringListCommands(cmds, this.player);
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Screen getScreen() {
        return this.screen;
    }
}