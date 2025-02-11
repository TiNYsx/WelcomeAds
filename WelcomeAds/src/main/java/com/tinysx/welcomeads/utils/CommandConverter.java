package com.tinysx.welcomeads.utils;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandConverter {

    public static void runStringListCommands(List<String> unConvertedString, Player player) {
        for (String key : unConvertedString) {
            runStringCommand(key, player);
        }
    }

    public static void runStringCommand(String unConvertedString, Player player) {
        String key = unConvertedString.replace("<player>", player.getName());
        if (key.contains("[console]")) {
            String cmdValue = key.replace("[console]", "");
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), cmdValue);
        } else if (key.contains("[player]")) {
            String cmdValue = key.replace("[player]", "");
            player.chat("/" + cmdValue);
        } else if (key.contains("[close]")) {
            player.closeInventory();
        } else if (key.contains("[sound]")) {
            String sound = key.replace("[sound]", "");
            player.playSound(player, sound, 1.0f, 1.0f);
        }
    }
}