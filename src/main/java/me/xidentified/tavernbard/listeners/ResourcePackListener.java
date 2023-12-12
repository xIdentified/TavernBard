package me.xidentified.tavernbard.listeners;

import me.xidentified.tavernbard.TavernBard;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ResourcePackListener implements Listener {
    private final TavernBard plugin;

    public ResourcePackListener(TavernBard plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyResourcePackToPlayer(player);
    }

    public void applyResourcePackToPlayer(Player player) {
        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean("resource_pack.external-host.enabled")) {
            String packLink = config.getString("resource_pack.external-host.pack-link");
            if (packLink != null && !packLink.isEmpty()) {
                player.setResourcePack(packLink);
                plugin.debugLog("Set resource pack to external link for player " + player.getName() + ": " + packLink);
            } else {
                plugin.debugLog("External resource pack link not set or is empty!");
            }
        } else {
            plugin.debugLog("Resource pack settings are not enabled in the config.");
        }
    }

}
