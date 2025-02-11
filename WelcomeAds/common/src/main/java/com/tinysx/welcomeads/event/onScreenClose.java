package com.tinysx.welcomeads.event;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

import com.tinysx.welcomeads.InventoryStorage;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.utils.CommandConverter;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public final class onScreenClose implements Listener {

    public Player player;
    public Screen screen;
    public String index;
    private final Plugin plugin;
    private final FileConfiguration config;

    public onScreenClose(InventoryCloseEvent event, WelcomeInventoryHolder holder, Plugin plugin) {

        this.player = (Player) event.getPlayer();
        this.screen = holder.getScreen();
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
        this.index = this.screen.getIndex();

        // sending background fade title to player
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(this.player, this.screen.getBackground() != null ? this.screen.getBackground() : "")), "", 0, this.screen.getBackgroundStay(), this.screen.getBackgroundFadeout());
        
        // getting, loading and remove the player's item storage
        InventoryStorage storage = InventoryStorage.getInventoryStorage(this.player);
        if (storage != null) {
            storage.unloadInventoryStorage(this.player);
            InventoryStorage.removeInventoryStorage(this.player);
        }
        
        // running event command
        List<String> cmds = this.config.getStringList("inventory." + this.index + ".events.onInventoryClose.commands");
        if (!cmds.isEmpty()) {
            CommandConverter.runStringListCommands(cmds, this.player);
        }
    }

    public Player getPlayer(){
        return this.player;
    }

    public Screen getScreen(){
        return this.screen;
    }
}