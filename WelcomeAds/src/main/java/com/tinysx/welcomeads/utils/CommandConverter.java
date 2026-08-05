package com.tinysx.welcomeads.utils;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class CommandConverter {

    private CommandConverter() {
    }

    public static void runStringListCommands(List<String> commands, Player player) {
        if (commands == null || commands.isEmpty() || player == null) {
            return;
        }
        for (String command : commands) {
            runStringCommand(command, player);
        }
    }

    public static void runStringCommand(String rawCommand, Player player) {
        if (rawCommand == null || rawCommand.trim().isEmpty() || player == null) {
            return;
        }

        String command = rawCommand.replace("<player>", player.getName());

        if (command.startsWith("[console]")) {
            String cmd = command.substring("[console]".length()).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (command.startsWith("[player]")) {
            String cmd = command.substring("[player]".length()).trim();
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            player.performCommand(cmd);
        } else if (command.equalsIgnoreCase("[close]")) {
            player.closeInventory();
        } else if (command.startsWith("[sound]")) {
            String soundName = command.substring("[sound]".length()).trim();
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                // Fallback for custom resource pack sounds or lower-case keys
                player.playSound(player.getLocation(), soundName.toLowerCase(), 1.0f, 1.0f);
            }
        } else if (command.startsWith("[message]")) {
            String message = command.substring("[message]".length()).trim();
            player.sendMessage(ColorUtil.format(player, message));
        }
    }
}
