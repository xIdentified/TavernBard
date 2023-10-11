package me.xidentified.tavernbard.listeners;

import me.xidentified.tavernbard.BardTrait;
import me.xidentified.tavernbard.SongSelectionGUI;
import me.xidentified.tavernbard.TavernBard;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BardInteractListener implements Listener {
    private final TavernBard plugin;

    public BardInteractListener(TavernBard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(BardTrait.class) && npc.getTrait(BardTrait.class).isBard()) {
            Player player = event.getClicker();
            SongSelectionGUI gui = new SongSelectionGUI(plugin, plugin.getSongManager(), npc.getUniqueId());
            player.openInventory(gui.getInventory());
            plugin.debugLog("Adding NPC with ID: " + npc.getUniqueId());
            plugin.getSongManager().bardNpcs.put(npc.getUniqueId(), npc);
        }
    }
}
