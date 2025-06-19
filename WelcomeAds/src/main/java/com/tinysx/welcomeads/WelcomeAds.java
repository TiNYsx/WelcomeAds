package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import com.tinysx.welcomeads.event.InventoryListener;
import com.tinysx.welcomeads.event.onScreenClose;
import com.tinysx.welcomeads.event.onScreenOpen;
import com.tinysx.welcomeads.utils.CommandConverter;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;


/**
 * WelcomeAds - A plugin for displaying advertisements to players in Minecraft.
 * @author TiNYsx
 * @version 1.8.2
 **/

public class WelcomeAds extends JavaPlugin implements Listener {
    // The list of inventory storages, contains all the inventories of players
    static List<InventoryStorage> inventoryStorages = new ArrayList<>();
    
    // Storaging player data, such as seen status for now
    static List<PlayerDataStorage> playerDataStorages = new ArrayList<>();

    WelcomeAds plugin = this;
    Config config;

    @SuppressWarnings("unused")
    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new Config(this.plugin);
        this.config.configLoad();
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin.");
            getPluginLoader().disablePlugin(this.plugin);
            return;
        }
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
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
                    new Screen(page, player).openTo(player);
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
                            if (player.getOpenInventory().getTopInventory()
                                    .getHolder() instanceof WelcomeInventoryHolder) {
                                player.closeInventory();
                            }
                            new Screen(index, player).openTo(player);
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
                    if (reloadPerm == null) {
                        reloadPerm = "welcomeads.reload";
                    }
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
        String loadtype = getConfig().getString("loadtype");
        if (loadtype != null && (loadtype.equalsIgnoreCase("onjoin") || loadtype.equalsIgnoreCase("both"))) {
            String page = getConfig().getString("joinpage");
            if (getConfig().getBoolean("inventory." + page + ".enable") != false) {
                if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                    if (getConfig().getBoolean("persession") == true) {
                        PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                        storage.getStatus().setJoinStatus(true);
                        if (storage.getSeenStatus() == true) {
                            return;
                        } else {
                            storage.setSeenStatus(true);
                        }
                    }
                    new Screen(page, player).openTo(player);
                } else {
                    PlayerDataStorage storage = new PlayerDataStorage(player);
                    storage.setSeenStatus(true);
                    storage.getStatus().setJoinStatus(true);
                    PlayerDataStorage.addPlayerDataStorage(storage);
                    new Screen(page, player).openTo(player);
                }
            }
        } else if (loadtype != null && (loadtype.equalsIgnoreCase("onresourcepack"))) {
            String page = getConfig().getString("joinpage");
            if (getConfig().getBoolean("inventory." + page + ".enable") != false) {
                if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                    PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                    if (storage.getStatus().getJoinStatus() == true) {
                        return;
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
    public void onResourcepackLoaded(PlayerResourcePackStatusEvent event) {
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            Player player = (Player) event.getPlayer();
            String loadtype = getConfig().getString("loadtype");
            if (loadtype != null && (loadtype.equalsIgnoreCase("onresourcepack") || loadtype.equalsIgnoreCase("both"))) {
                String page = getConfig().getString("joinpage");
                if (getConfig().getBoolean("inventory." + page + ".enable") != false){
                    if (PlayerDataStorage.isHavePlayerDataStorage(player)) {
                        PlayerDataStorage storage = PlayerDataStorage.getPlayerDataStorage(player);
                        if (getConfig().getBoolean("persession") == true) {
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
                                        new Screen(page, player).openTo(player);
                                        cancel();
                                    } else if (count < 20) {
                                        count++;
                                    } else {
                                        cancel();
                                        return;
                                    }
                                }
                            }.runTaskTimer(this, 10L, 10L);
                        } else {
                            storage.setSeenStatus(true);
                            new Screen(page, player).openTo(player);
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
                                    new Screen(page, player).openTo(player);
                                    cancel();
                                } else if (count < 20) {
                                    count++;
                                } else {
                                    cancel();
                                    return;
                                }
                            }
                        }.runTaskTimer(this, 10L, 10L);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            onScreenOpen openEvent = new onScreenOpen(event, holder);
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            onScreenClose closeEvent = new onScreenClose(event, holder);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                event.setCancelled(true);
            } else if (event.getClickedInventory() == event.getView().getTopInventory()) {
                Boolean isWelcomeAds = NBT.get(event.getCurrentItem(),
                        (Function<ReadableItemNBT, Boolean>) nbt -> nbt.getBoolean("welcomeads"));
                String adsId = NBT.get(event.getCurrentItem(), nbt -> (String) nbt.getString("adsid"));
                if ((isWelcomeAds != null && isWelcomeAds == true) && (adsId != null)) {
                    int slotIndex = event.getSlot();
                    String foundIndex = null;
                    ConfigurationSection itemsConfig = getConfig()
                            .getConfigurationSection("inventory." + adsId + ".items");
                    if (itemsConfig != null) {
                        for (String key : itemsConfig.getKeys(false)) {
                            if (itemsConfig.getInt(key + ".slot") == slotIndex) {
                                foundIndex = key;
                            }
                        }
                        if (foundIndex != null) {
                            event.setCancelled(true);
                            List<String> cmds = getConfig()
                                    .getStringList("inventory." + adsId + ".items." + foundIndex + ".commands");
                            Player player = (Player) event.getWhoClicked();
                            if (!cmds.isEmpty()) {
                                CommandConverter.runStringListCommands(cmds, player);
                            }
                        }
                    }
                }
            }
        }
    }
}
