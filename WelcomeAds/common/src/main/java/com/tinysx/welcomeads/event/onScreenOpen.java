package com.tinysx.welcomeads.event;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.Plugin;

import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.utils.CommandConverter;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public final class onScreenOpen {

    public Player player;
    public Screen screen;
    public String index;
    private final Plugin plugin;
    public String nextpage;
    public int delay;
    public FileConfiguration config;

    public onScreenOpen(InventoryOpenEvent event, WelcomeInventoryHolder holder, Plugin plugin) {
        this.player = (Player) event.getPlayer();
        this.screen = holder.getScreen();
        this.plugin = plugin;
        this.config = this.plugin.getConfig();
        this.index = this.screen.getIndex();        
        this.nextpage = this.config.getString("inventory." + this.index + ".nextpage");
        this.delay = this.config.getInt("inventory." + this.index + ".delay");

        // sending background title to player
        player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(this.player, this.screen.getBackground() != null ? this.screen.getBackground() : "")), "", 0, this.screen.getBackgroundStay(), this.screen.getBackgroundFadeout());
        
        // running event command
        List<String> cmds = this.config.getStringList("inventory." + this.index + ".events.onInventoryOpen.commands");
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