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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import com.tinysx.welcomeads.InventoryStorage;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeInventoryHolder;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;

public class WelcomeAds extends JavaPlugin implements Listener, TabCompleter {
    WelcomeAds plugin = this;
    SkinsRestorer skinsRestorerAPI;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
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
                        sender.sendMessage("§7[§e!§7] §fPlayer not found.");
                        return false;
                    }
                    ConfigurationSection windows = getConfig().getConfigurationSection("inventory");
                    if (windows == null) {
                        sender.sendMessage("§7[§e!§7] §fNo inventories configured in config.yml.");
                        return false;
                    }
                    if (windows.contains(index)) {
                        if (windows.getBoolean(index + ".enable")) {
                            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
                                player.closeInventory();                                
                            }
                            new Screen(index, player, this.plugin).openTo(player);
                        } else {
                            sender.sendMessage("§7[§e!§7] §fThis welcomeads inventory is disabled.");
                        }
                    } else {
                        sender.sendMessage("§7[§e!§7] §fInvalid inventory index.");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    sender.sendMessage("§7[§a!§7] §fConfiguration reloaded successfully!");
                    getLogger().info("Configuration reloaded.");
                    return true;
                }
            } else {
                sender.sendMessage("§7[§c!§7] §fYou do not have permission to use this command.");
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
        String page = getConfig().getString("joinpage");
        if (getConfig().getBoolean("inventory." + page + ".enable") != false) {
            new Screen(page, player, this.plugin).openTo(player);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            Player player = (Player) event.getPlayer();
            String adsId = holder.getIdentifier();
            Screen screen = holder.getScreen();

            if (holder != screen.getHolder()) {Bukkit.getLogger().warning("The holder is not the same");}
            
            String nextpage = getConfig().getString("inventory." + adsId + ".nextpage");
            String opensound = getConfig().getString("inventory." + adsId + ".open-sound");
            int delay = getConfig().getInt("inventory." + adsId + ".delay");
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, screen.getBackground() != null ? screen.getBackground() : "")), "", 0, screen.getBackgroundStay(), screen.getBackgroundFadeout());
            if (opensound != null) {player.playSound(player, opensound, 1.0f, 1.0f);}
            if (nextpage == null) {return;}
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
            }.runTaskTimer(this, Long.parseLong("" + delay), Long.parseLong("" + delay));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            Player player = (Player) event.getPlayer();
            Screen screen = holder.getScreen();
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, screen.getBackground() != null ? screen.getBackground() : "")), "", 0, screen.getBackgroundStay(), screen.getBackgroundFadeout());
            InventoryStorage storage = InventoryStorage.getInventoryStorage(player);
            if (storage != null) {
                storage.unloadInventoryStorage(player);
                InventoryStorage.removeInventoryStorage(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {return;}
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {event.setCancelled(true);}
        }
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            Boolean isWelcomeAds = NBT.get(event.getCurrentItem(), (Function<ReadableItemNBT, Boolean>) nbt -> nbt.getBoolean("welcomeads"));
            if (isWelcomeAds == null || !isWelcomeAds) {return;}
            String adsId = NBT.get(event.getCurrentItem(), nbt -> (String) nbt.getString("adsid"));
            if (adsId == null) {return;
            }
            if (isWelcomeAds == true) {
                int slotIndex = event.getSlot();
                String foundIndex = null;
                ConfigurationSection itemsConfig = getConfig().getConfigurationSection("inventory." + adsId + ".items");
                if (itemsConfig == null) {return;}
                for (String key : itemsConfig.getKeys(false)) {
                    if (itemsConfig.getInt(key + ".slot") == slotIndex) {foundIndex = key;}
                }
                if (foundIndex == null) {return;}
                event.setCancelled(true);
                List<String> cmds = getConfig().getStringList("inventory." + adsId + ".items." + foundIndex + ".commands");
                Player player = (Player) event.getWhoClicked();
                if (!cmds.isEmpty()) {
                    for (String key : cmds) {
                        key = key.replace("<player>", player.getName());
                        if (key.contains("[console]")) {
                            String cmdValue = key.replace("[console]", "");
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmdValue);
                        } else if (key.contains("[player]")) {
                            String cmdValue = key.replace("[player]", "");
                            player.chat("/" + cmdValue);
                        } else if (key.contains("[close]")) {
                            player.closeInventory();
                        } else if (key.contains("[sound]")) {
                            String sound = key.replace("[sound]", "");
                            player.playSound(player, sound, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }

    }
}
