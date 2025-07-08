package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import com.tinysx.welcomeads.event.InventoryListener;
import com.tinysx.welcomeads.event.PlayerListener;

import de.tr7zw.changeme.nbtapi.NBT;

/**
 * WelcomeAds - A plugin for displaying advertisements to players in Minecraft.
 *
 * @author TiNYsx
 * @version 1.9.1
 *
 */
public class WelcomeAds extends JavaPlugin implements Listener {

    // The list of inventory storages, contains all the inventories of players
    static List<InventoryStorage> inventoryStorages = new ArrayList<>();

    // Storaging player data, such as seen status for now
    static List<PlayerDataStorage> playerDataStorages = new ArrayList<>();

    WelcomeAds plugin = this;
    public Config config;

    @SuppressWarnings("unused")
    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new Config(this.plugin);
        this.config.reload();
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin.");
            getPluginLoader().disablePlugin(this.plugin);
            return;
        }
        int pluginId = 24657;
        Metrics metrics = new Metrics(this, pluginId);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getLogger().info("\u001B[0m");
        getLogger().info("\u001B[36;1mWelcomeAds plugin \u001B[32;1menabled!\u001B[0m");
        getLogger().info("\u001B[37;1mMade with love, by \u001B[32;1mTiNYsx\u001B[0m");
        getLogger().info("\u001B[0m");
    }

    @Override
    public void onDisable() {
        getLogger().info("WelcomeAds plugin disabled!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("welcomeads")) {
            switch (args.length) {
                case 1 -> {
                    List<String> completions = new ArrayList<>();
                    completions.add("open");
                    completions.add("reload");
                    return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
                }
                case 2 -> {
                    if (args[0].equalsIgnoreCase("open")) {
                        List<String> completions = new ArrayList<>();
                        ConfigurationSection windows = config.loadContainer().getConfigurationSection("container");
                        if (windows != null) {
                            for (String key : windows.getKeys(false)) {
                                completions.add(key);
                            }
                        }
                        return StringUtil.copyPartialMatches(args[1], completions, new ArrayList<>());
                    }
                }
                case 3 -> {
                    if (args[0].equalsIgnoreCase("open")) {
                        List<String> completions = new ArrayList<>();
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            completions.add(player.getName());
                        }
                        return StringUtil.copyPartialMatches(args[2], completions, new ArrayList<>());
                    }
                }
                default -> {
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("welcomeads")) {
            if (sender.hasPermission("welcomeads.use")) {
                if (args.length == 0) {
                    Player player = (Player) sender;
                    String page = getConfig().getString("joinpage");
                    new Screen(page, 0, player).openTo(player);
                    return true;
                }
                if (args.length >= 3 && args[0].equalsIgnoreCase("open")) {
                    String index = args[1];
                    Player player = Bukkit.getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage(config.loadLang("cmd-inv-playernotfound"));
                        return false;
                    }
                    ConfigurationSection windows = config.loadContainer().getConfigurationSection("container");
                    if (windows == null) {
                        sender.sendMessage(config.loadLang("cmd-inv-none"));
                        return false;
                    }
                    if (windows.contains(index)) {
                        if (player.getOpenInventory().getTopInventory()
                                .getHolder() instanceof WelcomeInventoryHolder) {
                            player.closeInventory();
                        }
                        new Screen(index, 0, player).openTo(player);
                    } else {
                        sender.sendMessage(config.loadLang("cmd-inv-invalidindex"));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    String reloadPerm = getConfig().getString("permission");
                    if (reloadPerm == null) {
                        reloadPerm = "welcomeads.reload";
                    }
                    if (sender.hasPermission(reloadPerm)) {
                        this.config.reload();
                        reloadConfig();
                        sender.sendMessage(config.loadLang("cmd-pl-reload"));
                        getLogger().info(config.loadLang("cmd-pl-reload"));
                        return true;
                    }
                }
            } else {
                sender.sendMessage(config.loadLang("cmd-perm-none"));
            }
        }
        return false;
    }
}
