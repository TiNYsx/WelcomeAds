package com.tinysx.welcomeads;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryStorage {

    private final Player player;
    private final Inventory inventory;

    public InventoryStorage(Player player) {
        this.inventory = Bukkit.createInventory(player, 36);
        this.player = player;
        loadInventoryStorage();
    }

    public void loadInventoryStorage() {
        int n = 0;
        for (ItemStack elem : this.player.getInventory().getStorageContents()) {
            if (n <= 36) {
                if (elem != null) {
                    this.inventory.setItem(n, elem.clone());
                    this.player.getInventory().clear(n);
                }
                n++;
            }
        }
    }

    public static void addInventoryStorage(InventoryStorage storage) {
        WelcomeAds.inventoryStorages.add(storage);
    }

    public static void removeInventoryStorage(InventoryStorage storage) {
        WelcomeAds.inventoryStorages.remove(storage);
    }

    public static InventoryStorage getInventoryStorage(Player player) {
        for (InventoryStorage storage : WelcomeAds.inventoryStorages) {
            if (storage.getPlayer().equals(player)) {
                return storage;
            }
        }
        return null;
    }

    public static boolean isHaveInventoryStorage(Player player) {
        return getInventoryStorage(player) != null;
    }

    public void unloadInventoryStorage() {
        this.player.getInventory().setStorageContents(this.inventory.getContents());
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Player getPlayer() {
        return this.player;
    }
}
