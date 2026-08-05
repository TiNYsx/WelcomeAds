package com.tinysx.welcomeads.event;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.tinysx.welcomeads.Config;
import com.tinysx.welcomeads.PlayerDataManager;
import com.tinysx.welcomeads.SafeInventoryManager;
import com.tinysx.welcomeads.Screen;
import com.tinysx.welcomeads.WelcomeAds;
import com.tinysx.welcomeads.WelcomeInventoryHolder;
import com.tinysx.welcomeads.utils.CommandConverter;

public class InventoryListener implements Listener {

    private final WelcomeAds welcomeads = WelcomeAds.getInstance();
    private final Config config = this.welcomeads.config;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onScreenClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder)) {
            return;
        }

        // Lock all clicks in both top and bottom inventories safely
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getCurrentItem() == null) {
            return;
        }

        // Only process click commands when clicking the top GUI
        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            WelcomeInventoryHolder holder = (WelcomeInventoryHolder) event.getView().getTopInventory().getHolder();
            if (holder != null && holder.getScreen() != null) {
                holder.getScreen().handleClick(event.getSlot(), (Player) event.getWhoClicked());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (SafeInventoryManager.hasHiddenInventory(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && SafeInventoryManager.hasHiddenInventory(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (SafeInventoryManager.hasHiddenInventory(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (SafeInventoryManager.hasHiddenInventory(event.getEntity())) {
            SafeInventoryManager.restorePlayerInventory(event.getEntity());
        }
    }

    @EventHandler
    public void onScreenOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder holder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Screen screen = holder.getScreen();
        if (screen == null) {
            return;
        }

        String container = screen.getContainer();
        int containerIndex = screen.getContainerIndex();
        int delay = config.loadContainer().getInt("container." + container + ".delay", 0);
        List<String> invList = config.loadContainer().getStringList("container." + container + ".inventory");
        boolean loop = config.loadContainer().getBoolean("container." + container + ".loop", false);

        // Execute onInventoryOpen commands
        List<String> openCmds = config.loadInventory()
                .getStringList("inventory." + screen.getIndex() + ".events.onInventoryOpen.commands");
        if (!openCmds.isEmpty()) {
            CommandConverter.runStringListCommands(openCmds, player);
        }

        if (delay <= 0) {
            return;
        }

        // Schedule container sequence progression safely
        if (containerIndex < invList.size() - 1 || loop) {
            int nextIndex = (containerIndex + 1 >= invList.size()) ? 0 : containerIndex + 1;
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()
                            && player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder) {
                        new Screen(container, nextIndex, player).openTo(player);
                    }
                }
            }.runTaskLater(this.welcomeads, delay);

            PlayerDataManager.getOrCreate(player.getUniqueId()).setTransitionTask(task);
        }
    }

    @EventHandler
    public void onScreenClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder holder)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Screen screen = holder.getScreen();
        if (screen == null) {
            return;
        }

        // Cancel any pending container transition timer
        PlayerDataManager.PlayerSessionData session = PlayerDataManager.get(player);
        if (session != null) {
            session.cancelTransitionTask();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    SafeInventoryManager.restorePlayerInventory(player);
                    return;
                }

                // If player has left WelcomeAds GUI completely, restore their hidden inventory
                if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof WelcomeInventoryHolder)) {
                    SafeInventoryManager.restorePlayerInventory(player);

                    if (config.loadInventory().getBoolean("inventory." + screen.getIndex() + ".force", false)) {
                        new Screen(screen.getContainer(), screen.getContainerIndex(), player).openTo(player);
                    } else {
                        List<String> closeCmds = config.loadInventory()
                                .getStringList("inventory." + screen.getIndex() + ".events.onInventoryClose.commands");
                        if (!closeCmds.isEmpty()) {
                            CommandConverter.runStringListCommands(closeCmds, player);
                        }
                    }
                }
            }
        }.runTaskLater(this.welcomeads, 1L);
    }
}
