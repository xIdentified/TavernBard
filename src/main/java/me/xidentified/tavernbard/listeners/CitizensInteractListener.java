package me.xidentified.tavernbard.listeners;

import me.xidentified.tavernbard.BardTrait;
import me.xidentified.tavernbard.TavernBard;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensInteractListener implements Listener {
    private final TavernBard plugin;

    public CitizensInteractListener(TavernBard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCitizensNPCInteract(NPCRightClickEvent event) {
        plugin.debugLog("Citizens NPC interacted: " + event.getNPC().getName());
        NPC npc = event.getNPC();
        if (npc.hasTrait(BardTrait.class)) {
            plugin.handleInteraction(npc.getMinecraftUniqueId(), event.getClicker());
        }
    }

}
