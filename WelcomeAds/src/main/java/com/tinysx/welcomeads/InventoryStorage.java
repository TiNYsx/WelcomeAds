package com.tinysx.welcomeads;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class InventoryStorage {
    private final Player player;
    private final Inventory inventory;
    static List<InventoryStorage> inventoryStorages = new ArrayList<>();

    public InventoryStorage(Player player) {
        this.inventory = Bukkit.createInventory(player, 54);
        this.inventory.setContents(player.getInventory().getContents());
        this.player = player;
    }

    // load player inventory to InventoryStorage
    public void loadInventoryStorage(Player player) {
        this.inventory.setContents(player.getInventory().getExtraContents());
        addInventoryStorage(this);
    }

    // unload InvetoryStorage to player inventory
    public void unloadInventoryStorage(Player player) {
        player.getInventory().setContents(this.inventory.getContents());
        removeInventoryStorage(player);
    }

    public static InventoryStorage createInventoryStorage(Player player) {
        InventoryStorage storage = new InventoryStorage(player);
        addInventoryStorage(storage);
        return storage;
    }

    private static void addInventoryStorage(InventoryStorage storage) {
        InventoryStorage.inventoryStorages.add(storage);
    }

    public static boolean isHaveInventoryStorage(Player player) {
        for (InventoryStorage storage : InventoryStorage.inventoryStorages) {
            if (storage.getPlayer().equals(player)) {
                return true;
            }
        }
        return false;
    }

    public static void removeInventoryStorage(Player player) {
        InventoryStorage storage = getInventoryStorage(player);
        if (storage != null) {
            InventoryStorage.inventoryStorages.remove(storage);
        }
    }

    public static InventoryStorage getInventoryStorage(Player player) {
        for (InventoryStorage storage : InventoryStorage.inventoryStorages) {
            if (storage.getPlayer().equals(player)) {
                return storage;
            }
        }
        return null;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public Player getPlayer() {
        return this.player;
    }

    public InventoryStorage clone(Player player) {
        return new InventoryStorage(player);
    }
}