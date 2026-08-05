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
import com.tinysx.welcomeads.utils.ColorUtil;

/**
 * WelcomeAds - A premium welcome screen and announcement plugin for Minecraft.
 *
 * @author TiNYsx
 * @version 2.1
 */
public class WelcomeAds extends JavaPlugin implements Listener {

    private static WelcomeAds instance;
    public Config config;

    public static WelcomeAds getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Detect PlaceholderAPI Hook
        boolean hasPapi = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        ColorUtil.setPlaceholderApiPresent(hasPapi);

        this.config = new Config(this);
        this.config.reload();

        int pluginId = 24657;
        new Metrics(this, pluginId);

        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        getLogger().info("\u001B[0m");
        getLogger().info("\u001B[36;1mWelcomeAds plugin \u001B[32;1menabled!\u001B[0m");
        getLogger().info("\u001B[37;1mMade with love, by \u001B[32;1mTiNYsx\u001B[0m");
        getLogger().info("\u001B[0m");
    }

    @Override
    public void onDisable() {
        // Close all active WelcomeAds inventories and purge transition tasks
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
                player.closeInventory();
            }
        }
        SafeInventoryManager.restoreAll();
        PlayerDataManager.clearAll();
        getLogger().info("WelcomeAds plugin disabled cleanly!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("welcomeads")) {
            return null;
        }

        switch (args.length) {
            case 1 -> {
                return StringUtil.copyPartialMatches(args[0], List.of("open", "reload"), new ArrayList<>());
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("open")) {
                    List<String> completions = new ArrayList<>();
                    ConfigurationSection windows = config.loadContainer().getConfigurationSection("container");
                    if (windows != null) {
                        completions.addAll(windows.getKeys(false));
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
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!label.equalsIgnoreCase("welcomeads")) {
            return false;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can open the welcome screen GUI directly.");
                return true;
            }
            if (!player.hasPermission("welcomeads.use")) {
                player.sendMessage(config.loadLang("cmd-perm-none", player));
                return true;
            }
            String page = getConfig().getString("joinpage", "welcome-1");
            new Screen(page, 0, player).openTo(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            Player pSender = (sender instanceof Player p) ? p : null;
            if (!sender.hasPermission("welcomeads.open") && !sender.hasPermission("welcomeads.admin")) {
                sender.sendMessage(config.loadLang("cmd-perm-none", pSender));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /welcomeads open <container> <player>");
                return true;
            }
            String containerName = args[1];
            Player targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage(config.loadLang("cmd-inv-playernotfound", pSender));
                return true;
            }
            ConfigurationSection windows = config.loadContainer().getConfigurationSection("container");
            if (windows == null || !windows.contains(containerName)) {
                sender.sendMessage(config.loadLang("cmd-inv-invalidindex", pSender));
                return true;
            }
            new Screen(containerName, 0, targetPlayer).openTo(targetPlayer);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            Player pSender = (sender instanceof Player p) ? p : null;
            String reloadPerm = getConfig().getString("permission", "welcomeads.reload");
            if (sender.hasPermission(reloadPerm) || sender.hasPermission("welcomeads.admin")) {
                this.config.reload();
                sender.sendMessage(config.loadLang("cmd-pl-reload", pSender));
                getLogger().info(config.loadLang("cmd-pl-reload"));
                return true;
            } else {
                sender.sendMessage(config.loadLang("cmd-perm-none", pSender));
                return true;
            }
        }

        return false;
    }
}
