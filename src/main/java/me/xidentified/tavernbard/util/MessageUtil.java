package me.xidentified.tavernbard.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
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
        if (isPaper) {
            // For Paper, parse the message as a Component
            return miniMessage.deserialize(message);
        } else {
            // For Spigot, create a simple text Component from the message
            return Component.text(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public void sendParsedMessage(Player player, String message) {
        if (isPaper()) {
            // If Paper, use Component-based sendMessage
            player.sendMessage(parse(message));
        } else {
            // For Spigot, convert Component to String and use String-based sendMessage
            String plainMessage = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(plainMessage);
        }
    }

    public String getConfigMessage(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public String convertToString(String message) {
        if (isPaper) {
            Component component = parse(message);
            // Convert Component to String
            return LegacyComponentSerializer.legacySection().serialize(component);
        } else {
            // Directly return the message for Spigot servers
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }

    public Component convertToComponent(String message) {
        if (isPaper()) {
            return parse(message);
        } else {
            // For Spigot, convert String to simple text component
            return Component.text(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

}