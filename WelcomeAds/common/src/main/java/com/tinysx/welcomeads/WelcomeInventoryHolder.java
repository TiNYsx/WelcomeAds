package com.tinysx.welcomeads;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class WelcomeInventoryHolder implements InventoryHolder {
    private final String identifier;
    private final Screen screen;

    public WelcomeInventoryHolder(Screen screen) {
        this.identifier = screen.getIndex();
        this.screen = screen;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Inventory getInventory() {
        return this.screen.getScreenInventory(null);
    }

    public Screen getScreen() {
        return this.screen;
    }
}
