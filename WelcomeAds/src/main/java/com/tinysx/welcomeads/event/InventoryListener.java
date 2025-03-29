package com.tinysx.welcomeads.event;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import com.tinysx.welcomeads.WelcomeInventoryHolder;

public class InventoryListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WelcomeInventoryHolder)) return;
        
        // Cancel all events immediately
        event.setCancelled(true);
        
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        WelcomeInventoryHolder holder = (WelcomeInventoryHolder) event.getInventory().getHolder();
        holder.getScreen().handleClick(event.getSlot(), (Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WelcomeInventoryHolder) {
            event.setCancelled(true);
        }
    }
}