package com.tinysx.welcomeads.event;

import java.util.List;
import java.util.function.Function;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.tinysx.welcomeads.Config;
import com.tinysx.welcomeads.InventoryStorage;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeAds;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.utils.CommandConverter;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

public class InventoryListener implements Listener {

    private final WelcomeAds welcomeads = WelcomeAds.getPlugin(WelcomeAds.class);
    Config config = this.welcomeads.config;

    @EventHandler
    public void onScreenClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        WelcomeInventoryHolder holder = (WelcomeInventoryHolder) event.getInventory().getHolder();
        holder.getScreen().handleClick(event.getSlot(), (Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WelcomeInventoryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onScreenClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Screen screen = ((WelcomeInventoryHolder) event.getInventory().getHolder()).getScreen();
        String index = screen.getIndex();
        String container = screen.getContainer();
        Integer containerIndex = screen.getContainerIndex();

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&',
                        PlaceholderAPI.setPlaceholders(player,
                                screen.getBackground() != null ? screen.getBackground() : "")),
                "", 0, 20, 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder == false) {
                    if (config.loadInventory().getBoolean("inventory." + index + ".force") == true) {
                        player.closeInventory();
                        new Screen(container, containerIndex, player).openTo(player);
                    } else {
                        player.sendTitle(
                                ChatColor.translateAlternateColorCodes('&',
                                        PlaceholderAPI.setPlaceholders(player,
                                                screen.getBackground() != null ? screen.getBackground() : "")),
                                "", screen.getBackgroundFadein(), screen.getBackgroundStay(), screen.getBackgroundFadeout());

                        if (InventoryStorage.isHaveInventoryStorage(player)) {
                            InventoryStorage.getInventoryStorage(player).unloadInventoryStorage();
                            InventoryStorage.removeInventoryStorage(InventoryStorage.getInventoryStorage(player));
                        }

                        List<String> cmds = config.loadInventory()
                                .getStringList("inventory." + index + ".events.onInventoryClose.commands");
                        if (!cmds.isEmpty()) {
                            CommandConverter.runStringListCommands(cmds, player);
                        }
                    }
                }
            }
        }.runTaskLater(this.welcomeads, 0L);
    }

    @EventHandler
    public void onScreenOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Screen screen = ((WelcomeInventoryHolder) event.getInventory().getHolder()).getScreen();
        String index = screen.getIndex();
        String container = screen.getContainer();
        Integer containerIndex = screen.getContainerIndex();
        Long delay = Long.parseLong("" + config.loadContainer().getInt("container." + container + ".delay"));
        if (delay < 0) {
            delay = 0L;
        }

        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&',
                        PlaceholderAPI.setPlaceholders(player,
                                screen.getBackground() != null ? screen.getBackground() : "")),
                "", screen.getBackgroundFadein(), screen.getBackgroundStay(), screen.getBackgroundFadeout());

        List<String> cmds = config.loadInventory().getStringList("inventory." + index + ".events.onInventoryOpen.commands");
        if (!cmds.isEmpty()) {
            CommandConverter.runStringListCommands(cmds, player);
        }
        if (delay == 0L) {
            return;
        }
        if (containerIndex < config.loadContainer().getStringList("container." + container + ".inventory").size() - 1) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (event.getPlayer().getOpenInventory().getTopInventory() != event.getView().getTopInventory()) {
                        cancel();
                    } else {
                        event.getPlayer().closeInventory();
                        new Screen(container, (containerIndex + 1), (Player) event.getPlayer()).openTo((Player) event.getPlayer());
                    }
                }
            }.runTaskTimer(this.welcomeads, delay, delay);
        } else if (containerIndex == config.loadContainer().getStringList("container." + container + ".inventory").size() - 1) {
            if (config.loadContainer().getBoolean("container." + container + ".loop")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (event.getPlayer().getOpenInventory().getTopInventory() != event.getView().getTopInventory()) {
                            cancel();
                        } else {
                            event.getPlayer().closeInventory();
                            new Screen(container, (containerIndex + 1), (Player) event.getPlayer()).openTo((Player) event.getPlayer());
                        }
                    }
                }.runTaskTimer(this.welcomeads, delay, delay);
            }
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
                    ConfigurationSection itemsConfig = config.loadInventory().getConfigurationSection("inventory." + adsId + ".items");
                    if (itemsConfig != null) {
                        for (String key : itemsConfig.getKeys(false)) {
                            if (itemsConfig.getInt(key + ".slot") == slotIndex) {
                                foundIndex = key;
                            }
                        }
                        if (foundIndex != null) {
                            event.setCancelled(true);
                            List<String> cmds = config.loadInventory().getStringList("inventory." + adsId + ".items." + foundIndex + ".commands");
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
