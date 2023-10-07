package me.xidentified.tavernbard.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MessageUtil {
    private final MiniMessage miniMessage;
    private final FileConfiguration config;

    public MessageUtil(FileConfiguration config) {
        this.miniMessage = MiniMessage.miniMessage();
        this.config = config;
    }

    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    public String getConfigMessage(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public void sendParsedMessage(Player player, String message) {
        player.sendMessage(parse(message));
    }
}