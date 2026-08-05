package com.tinysx.welcomeads;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WelcomeInventoryHolder implements InventoryHolder {

    private final Screen screen;

    public WelcomeInventoryHolder(Screen screen) {
        this.screen = screen;
    }

    public String getIdentifier() {
        return this.screen != null ? this.screen.getIndex() : "";
    }

    @Override
    public Inventory getInventory() {
        return this.screen != null ? this.screen.getInventory() : null;
    }

    public Screen getScreen() {
        return this.screen;
    }
}
