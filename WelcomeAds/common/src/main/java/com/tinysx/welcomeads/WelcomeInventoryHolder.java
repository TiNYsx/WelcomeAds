package com.tinysx.welcomeads;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WelcomeInventoryHolder implements InventoryHolder {
    private final String identifier;

    public WelcomeInventoryHolder(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used for this purpose
    }
}
