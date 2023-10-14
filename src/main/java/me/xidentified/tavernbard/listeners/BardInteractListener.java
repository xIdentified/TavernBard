package me.xidentified.tavernbard.listeners;

import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.xidentified.tavernbard.BardTrait;
import me.xidentified.tavernbard.SongSelectionGUI;
import me.xidentified.tavernbard.TavernBard;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class BardInteractListener implements Listener {
    private final TavernBard plugin;

    public BardInteractListener(TavernBard plugin) {
        this.plugin = plugin;
    }

    // Handle interactions with Citizens NPCs
    @EventHandler
    public void onRightClickNPC(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(BardTrait.class))
            handleInteraction(npc.getUniqueId(), event.getClicker());
    }

    @EventHandler
    public void onMythicMobInteract(MythicMobInteractEvent event) {
        ActiveMob interactedMob = event.getActiveMob();
        if (plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            if (interactedMob.getType().getConfig().getBoolean("Options.IsBard"))
                handleInteraction(interactedMob.getUniqueId(), event.getPlayer());
        }
    }

    public void handleInteraction(UUID bardEntityId, Player player) {
        plugin.debugLog("handleInteraction method fired");

        SongSelectionGUI gui = new SongSelectionGUI(plugin, plugin.getSongManager(), bardEntityId);
        player.openInventory(gui.getInventory());

        if (!plugin.getSongManager().bardNpcs.containsKey(bardEntityId)) {
            plugin.debugLog("Adding entity with ID: " + bardEntityId);
            if (bardEntityId != null) {
                plugin.getSongManager().bardNpcs.put(bardEntityId, player.getUniqueId());
            }
        } else {
            plugin.debugLog("Entity with ID: " + bardEntityId + " already added.");
        }
    }

}
