package com.tinysx.welcomeads.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import com.tinysx.welcomeads.Config;
import com.tinysx.welcomeads.InventoryStorage;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.event.onScreenClose;
import com.tinysx.welcomeads.event.onScreenOpen;
import com.tinysx.welcomeads.utils.CommandConverter;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;

public class WelcomeAds extends JavaPlugin implements Listener, TabCompleter {
    WelcomeAds plugin = this;
    SkinsRestorer skinsRestorerAPI;
    Config config;
    
    @Override
    public void onEnable() {
        this.config = new Config(this.plugin);
        this.config.configLoad();
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin.");
            getPluginLoader().disablePlugin(this.plugin);
            return;
        }
        if (getServer().getPluginManager().getPlugin("SkinsRestorer") == null) {
            getLogger().severe("SkinsRestorer plugin not found! Disabling WelcomeAds.");
            getPluginLoader().disablePlugin(this.plugin);
            return;
        }
        this.skinsRestorerAPI = SkinsRestorerProvider.get();
        if (this.skinsRestorerAPI == null) {
            getLogger().warning("SkinRestorer is not loading, the plugin might not work correctly.");
            return;
        }
        int pluginId = 24657;
        Metrics metrics = new Metrics(this, pluginId);
        getServer().getPluginManager().registerEvents(this, this.plugin);
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
                        ConfigurationSection windows = getConfig().getConfigurationSection("inventory");
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
                    new Screen(page, player, this.plugin).openTo(player);
                    return true;
                }
                if (args.length >= 3 && args[0].equalsIgnoreCase("open")) {
                    String index = args[1];
                    Player player = Bukkit.getPlayer(args[2]);
                    if (player == null) {
                        sender.sendMessage(config.loadLang("cmd-inv-playernotfound"));
                        return false;
                    }
                    ConfigurationSection windows = getConfig().getConfigurationSection("inventory");
                    if (windows == null) {
                        sender.sendMessage(config.loadLang("cmd-inv-none"));
                        return false;
                    }
                    if (windows.contains(index)) {
                        if (windows.getBoolean(index + ".enable")) {
                            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
                                player.closeInventory();                                
                            }
                            new Screen(index, player, this.plugin).openTo(player);
                        } else {
                            sender.sendMessage(config.loadLang("cmd-inv-disable"));
                        }
                    } else {
                        sender.sendMessage(config.loadLang("cmd-inv-invalidindex"));
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    String reloadPerm = getConfig().getString("permission");
                    if (reloadPerm == null) {reloadPerm = "welcomeads.reload";}
                    if (sender.hasPermission(reloadPerm)) {
                        reloadConfig();
                        this.config.reload();
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = (Player) event.getPlayer();
        if (InventoryStorage.isHaveInventoryStorage(player)) {
            InventoryStorage storage = InventoryStorage.getInventoryStorage(player);
            storage.unloadInventoryStorage(player);
            InventoryStorage.removeInventoryStorage(player);
        }
        String loadtype = getConfig().getString("loadtype");
        if (loadtype != null && (loadtype.equalsIgnoreCase("onjoin") || loadtype.equalsIgnoreCase("both"))) {
            String page = getConfig().getString("joinpage");
            if (getConfig().getBoolean("inventory." + page + ".enable") != false) {
                new Screen(page, player, this.plugin).openTo(player);
            }
        }
    }

    @EventHandler
    public void onResourcepackLoaded(PlayerResourcePackStatusEvent event) {
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            Player player = (Player) event.getPlayer();
            String loadtype = getConfig().getString("loadtype");
            if (loadtype != null && (loadtype.equalsIgnoreCase("onresourcepack") || loadtype.equalsIgnoreCase("both"))) {
                String page = getConfig().getString("joinpage");
                if (getConfig().getBoolean("inventory." + page + ".enable") != false) {
                    new Screen(page, player, this.plugin).openTo(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            String adsId = holder.getIdentifier();
            String nextpage = getConfig().getString("inventory." + adsId + ".nextpage");
            int delay = getConfig().getInt("inventory." + adsId + ".delay");
            if (nextpage != null) {
                onScreenOpen openEvent = new onScreenOpen(event, holder, this.plugin);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (event.getPlayer().getOpenInventory().getTopInventory() != event.getView().getTopInventory()) {cancel();} 
                        else {
                            if (getConfig().getBoolean("inventory." + nextpage + ".enable") != false) {
                                event.getPlayer().closeInventory();
                                new Screen(nextpage, (Player) event.getPlayer(), WelcomeAds.this.plugin).openTo((Player) event.getPlayer());
                            } 
                            else {cancel();}
                        }
                    }
                }.runTaskTimer(this, Long.parseLong("" + delay), 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // checking if the closed inventory is an instance of WelcomeAds inventory
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            onScreenClose closeEvent = new onScreenClose(event, holder, this.plugin);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // is air item checking
        if (event.getCurrentItem() == null) {return;}
        // is WelcomeAds's inventory checking
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            // is player's inventory click cancelling
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                event.setCancelled(true);
            }
            // is player's top inventory checking
            else if (event.getClickedInventory() == event.getView().getTopInventory()) {
                // making sure if it's the item from welcomeads using NBT // String("adsid"), Boolean("welcomeads", true)
                Boolean isWelcomeAds = NBT.get(event.getCurrentItem(), (Function<ReadableItemNBT, Boolean>) nbt -> nbt.getBoolean("welcomeads"));
                String adsId = NBT.get(event.getCurrentItem(), nbt -> (String) nbt.getString("adsid"));
                // checking if the boolean nbt is true and ads id is not null
                if ((isWelcomeAds != null && isWelcomeAds == true) && (adsId != null)) {
                    int slotIndex = event.getSlot();
                    String foundIndex = null;
                    ConfigurationSection itemsConfig = getConfig().getConfigurationSection("inventory." + adsId + ".items");
                    // checking for the items config, if there is no config, do nothing
                    if (itemsConfig != null) {
                        // finding the slot with the same slot index 
                        for (String key : itemsConfig.getKeys(false)) {
                            if (itemsConfig.getInt(key + ".slot") == slotIndex) {foundIndex = key;}
                        }
                        // if found, cancel the click event get the command of the clicked item
                        if (foundIndex != null) {
                            event.setCancelled(true);
                            // getting the commands from the configs
                            List<String> cmds = getConfig().getStringList("inventory." + adsId + ".items." + foundIndex + ".commands");
                            Player player = (Player) event.getWhoClicked();
                            // checking if there is any commands in the config for the clicked item
                            if (!cmds.isEmpty()) {
                                // run the command list
                                CommandConverter.runStringListCommands(cmds, player);
                            }
                        }
                    }
                }
            }
        }
    }
}
