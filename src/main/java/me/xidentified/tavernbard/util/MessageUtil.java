package me.xidentified.tavernbard.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.ChatColor;

public class MessageUtil {
    private final MiniMessage miniMessage;
    private final FileConfiguration config;
    private final boolean isPaper;

    public MessageUtil(FileConfiguration config) {
        this.miniMessage = MiniMessage.miniMessage();
        this.config = config;
        this.isPaper = checkIfPaperServer();
    }

    private boolean checkIfPaperServer() {
        try {
            // Attempt to access a Paper-specific class or method
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isPaper() {
        return this.isPaper;
    }

    public Component parse(String message) {
        if (isPaper()) {
            // For Paper, parse using MiniMessage
            return miniMessage.deserialize(message);
        } else {
            // For Spigot, translate color codes and convert to a Component
            String translatedMessage = ChatColor.translateAlternateColorCodes('&', message);
            return Component.text(translatedMessage);
        }
    }

    public String getConfigMessage(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    // Trying to get messages to work on both Spigot and Paper servers regardless of how you enter color codes
    public String convertToUniversalFormat(String message) {
        Component component;

        // First, try parsing as MiniMessage (handles MiniMessage-style tags)
        try {
            component = miniMessage.deserialize(message);
        } catch (Exception e) {
            // If parsing fails, assume it's traditional color codes and convert
            String translatedMessage = ChatColor.translateAlternateColorCodes('&', message);
            component = Component.text(translatedMessage);
        }

        // Convert the Component to a universally compatible String format
        String universalString = LegacyComponentSerializer.legacySection().serialize(component);
        return universalString;
    }

}