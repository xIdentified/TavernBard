package me.xidentified.tavernbard.listeners;

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

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(BardTrait.class) && npc.getTrait(BardTrait.class).isBard()) {
            UUID npcId = npc.getUniqueId();
            Player player = event.getClicker();
            SongSelectionGUI gui = new SongSelectionGUI(plugin, plugin.getSongManager(), npcId);
            player.openInventory(gui.getInventory());

            if (!plugin.getSongManager().bardNpcs.containsKey(npcId)) {
                plugin.debugLog("Adding NPC with ID: " + npcId);
                plugin.getSongManager().bardNpcs.put(npcId, npc);
            } else {
                plugin.debugLog("NPC with ID: " + npcId + " already added.");
            }
        }
    }
}
