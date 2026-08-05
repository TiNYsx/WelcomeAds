package com.tinysx.welcomeads.utils;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;

public final class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static boolean placeholderApiPresent = false;

    private ColorUtil() {
    }

    public static void setPlaceholderApiPresent(boolean present) {
        placeholderApiPresent = present;
    }

    public static String format(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. PlaceholderAPI hook if available
        String parsed = text;
        if (placeholderApiPresent && player != null) {
            try {
                parsed = PlaceholderAPI.setPlaceholders(player, text);
            } catch (Exception ignored) {
                parsed = text;
            }
        }

        // 2. MiniMessage formatting (<gradient>, <red>, <#RRGGBB>, etc.)
        try {
            Component component = MINI_MESSAGE.deserialize(parsed);
            parsed = LEGACY_SERIALIZER.serialize(component);
        } catch (Exception ignored) {
            // Fallback if raw text has unmatched tags
        }

        // 3. Legacy '&' color code translation
        return ChatColor.translateAlternateColorCodes('&', parsed);
    }
}
