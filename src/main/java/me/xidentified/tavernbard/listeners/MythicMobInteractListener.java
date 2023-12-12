package me.xidentified.tavernbard.listeners;

import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.xidentified.tavernbard.TavernBard;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobInteractListener implements Listener {
    private final TavernBard plugin;
    public MythicMobInteractListener(TavernBard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobInteract(MythicMobInteractEvent event) {
        ActiveMob interactedMob = event.getActiveMob();
        plugin.debugLog("MythicMob interacted with: " + interactedMob.getUniqueId());

        if (!Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            plugin.debugLog("MythicMobs is not enabled.");
            return;
        }

        if (!interactedMob.getType().getConfig().getBoolean("Options.IsBard")) {
            plugin.debugLog("MythicMob " + interactedMob.getUniqueId() + " is not a bard according to its configuration.");
            return;
        }

        plugin.debugLog("Attempting to handle interaction for MythicMob: " + interactedMob.getUniqueId());
        plugin.handleInteraction(interactedMob.getUniqueId(), event.getPlayer());
    }

}
