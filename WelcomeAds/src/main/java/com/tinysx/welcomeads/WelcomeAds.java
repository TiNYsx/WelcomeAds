package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.crashdemons.playerheads.api.PlayerHeads;
import com.github.crashdemons.playerheads.api.PlayerHeadsAPI;

import de.tr7zw.changeme.nbtapi.NBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class WelcomeAds extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        if (!NBT.preloadApi()) {
            getLogger().warning("NBT-API wasn't initialized properly, disabling the plugin.");
            getPluginLoader().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlayerHeads") == null) {
            getLogger().severe("PlayerHeads is not installed! Disabling WelcomeAds.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WelcomeAds plugin enabled!");
    }


    @Override
    public void onDisable() {
        getLogger().info("WelcomeAds plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("welcomeads")) {
            if (sender.hasPermission("welcomeads.use")){
                if (args.length == 0) {
                    Player player = (Player) sender;
                    String page = getConfig().getString("joinpage");
                    Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "welcomeads open " + page + " " + player.getName());
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
                            openWelcomeScreen(player, index);
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
            }
            else {
                sender.sendMessage("§7[§c!§7] §fYou do not have permission to use this command.");
            }
        }
        return false;
    }

    private void openWelcomeScreen(Player player, String index) {
        ConfigurationSection itemsConfig = getConfig().getConfigurationSection("inventory." + index + ".items");
        String title = getConfig().getString("inventory." + index + ".title");
        String background = getConfig().getString("background.text");
        String welcometitle = ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, "&f" + background + "&f" + title));
        Inventory welcomeInventory = Bukkit.createInventory(new WelcomeInventoryHolder(index), 54, welcometitle);
        
        if (itemsConfig == null) {
            player.sendMessage("§7[§e!§7] §fThe config for the index §e" + index + "§f is empty, please read the document.");
        }
        else {
            for (String key : itemsConfig.getKeys(false)) {
                String itemName = itemsConfig.getString(key + ".name");
                String itemMaterial = itemsConfig.getString(key + ".material");
                int itemModelData = itemsConfig.getInt(key + ".modeldata");
                int itemIndex = itemsConfig.getInt(key + ".slot");
                String headOwner = player.getName();
                List<String> itemLore = itemsConfig.getStringList(key + ".lore");
                if (itemMaterial == null || itemName == null){
                    continue;
                }
                if (itemLore.isEmpty()) {
                    itemLore = new ArrayList<>();
                }
                if (itemMaterial.contains(":")){
                    String material = itemMaterial.split(":")[0];
                    headOwner = itemMaterial.split(":")[1];
                    if (headOwner == null) {
                        headOwner = player.getName();
                    }
                    itemMaterial = material;
                }
                ItemStack item = new ItemStack(Material.getMaterial(itemMaterial));
                if (Material.getMaterial(itemMaterial) == Material.PLAYER_HEAD) {
                    PlayerHeadsAPI api = PlayerHeads.getApiInstance();
                    item = api.getHeadItem(headOwner, 1, true);
                    ItemMeta skullmeta = item.getItemMeta();
                    if (skullmeta != null) {
                        skullmeta.setCustomModelData(itemModelData);
                        skullmeta.setDisplayName(PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemName)));
                        for (int i = 0; i < itemLore.size(); i++){
                            itemLore.set(i, PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
                        }
                        skullmeta.setLore(itemLore);
                        item.setItemMeta(skullmeta);
                    }
                }
                else{
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null){
                        if (itemModelData >= 0){
                            meta.setCustomModelData(itemModelData);
                            item.setItemMeta(meta);
                            meta.setDisplayName(PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemName)));
                            for (int i = 0; i < itemLore.size(); i++){
                                itemLore.set(i, PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', itemLore.get(i))));
                            }
                            meta.setLore(itemLore);
                            item.setItemMeta(meta);
                        }
                    }   
                }
                NBT.modify(item, nbt -> {
                    nbt.setString("adsid", index);
                    nbt.setBoolean("welcomeads", true);
                });

                welcomeInventory.setItem(itemIndex, item);
            }
            player.openInventory(welcomeInventory);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = (Player) event.getPlayer();
        String page = getConfig().getString("joinpage");
        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "welcomeads open " + page + " " + player.getName());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            Player player = (Player) event.getPlayer();
            String adsId = holder.getIdentifier();
            String nextpage = getConfig().getString("inventory." + adsId + ".nextpage");
            String opensound = getConfig().getString("inventory." + adsId + ".open-sound");
            if (opensound != null) {
                player.playSound(player, opensound, 1.0f, 1.0f);
            }

            if (getConfig().getBoolean("background.enable") == true){
                String background = getConfig().getString("background.text");
                int stay = getConfig().getInt("background.stay");
                int out = getConfig().getInt("background.fadeout");
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, background != null ? background : "")), "", 0, stay+1000, out);
            }

            int delay = getConfig().getInt("inventory." + adsId + ".delay");
            if (nextpage == null) return;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (event.getPlayer().getOpenInventory().getTopInventory() != event.getView().getTopInventory()) {
                        cancel();
                    } else {
                        openWelcomeScreen((Player) event.getPlayer(), nextpage);
                    }
                }
            }.runTaskTimer(this, Long.parseLong(""+delay), Long.parseLong(""+delay));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            Player player = (Player) event.getPlayer();
            if (getConfig().getBoolean("background.enable") == true){
                String background = getConfig().getString("background.text");
                int stay = getConfig().getInt("background.stay");
                int out = getConfig().getInt("background.fadeout");
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, background != null ? background : "")), "", 0, stay, out);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder holder) {
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                event.setCancelled(true);
            }
        }

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            Boolean isWelcomeAds = NBT.get(event.getCurrentItem(), nbt -> (Boolean) nbt.getBoolean("welcomeads"));
            if (isWelcomeAds == null || !isWelcomeAds) return;
            String adsId = NBT.get(event.getCurrentItem(), nbt -> (String) nbt.getString("adsid"));
            if (adsId == null) return;
            if (isWelcomeAds == true) {
                int slotIndex = event.getSlot();
                String foundIndex = null;
                ConfigurationSection itemsConfig = getConfig().getConfigurationSection("inventory." + adsId + ".items");
                if (itemsConfig == null) return;
                for (String key : itemsConfig.getKeys(false)){
                    if (itemsConfig.getInt(key + ".slot") == slotIndex){
                        foundIndex = key;
                    }
                }
                if (foundIndex == null) return;
                event.setCancelled(true);
                List<String> cmds = getConfig().getStringList("inventory." + adsId + ".items." + foundIndex + ".commands");
                Player player = (Player) event.getWhoClicked();
                if (!cmds.isEmpty()) {
                    for (String key : cmds){
                        key = key.replace("<player>", player.getName());
                        if (key.contains("[console]")) {
                            String cmdValue = key.replace("[console]", "");
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmdValue);
                        }
                        else if (key.contains("[player]")) {
                            String cmdValue = key.replace("[player]", "");
                            player.chat("/"+cmdValue);
                        }
                        else if (key.contains("[close]")) {
                            player.closeInventory();
                        }
                        else if (key.contains("[sound]")) {
                            String sound = key.replace("[sound]", "");
                            player.playSound(player, sound, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }
        
    }
}
