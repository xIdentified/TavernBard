package me.xidentified.tavernbard.listeners;

import me.xidentified.tavernbard.Song;
import me.xidentified.tavernbard.SongSelectionGUI;
import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.managers.SongManager;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EventListener implements Listener {
    private final SongManager songManager;
    private final TavernBard plugin;

    public EventListener(TavernBard plugin, SongManager songManager) {
        this.plugin = plugin;
        this.songManager = songManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        if (holder instanceof SongSelectionGUI songSelectionGUI) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) {
                plugin.debugLog("Item has no meta");
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (container.has(new NamespacedKey(plugin, "songName"), PersistentDataType.STRING)) {
                String actualSongName = container.get(new NamespacedKey(plugin, "songName"), PersistentDataType.STRING);
                Song selectedSong = songManager.getSongByName(actualSongName);
                if (selectedSong != null) {
                    plugin.debugLog("Song selected: " + selectedSong.getDisplayName());
                    NPC bardNpc = songSelectionGUI.getBardNpc();
                    songManager.playSongForNearbyPlayers(player, bardNpc, selectedSong, true);
                    player.closeInventory();
                } else {
                    plugin.debugLog("Song not found for name: " + actualSongName);
                }
            } else if (container.has(new NamespacedKey(plugin, "action"), PersistentDataType.STRING)) {
                String action = container.get(new NamespacedKey(plugin, "action"), PersistentDataType.STRING);
                if (action == null) {
                    plugin.debugLog("Action is null - onInventoryClick");
                    return;
                }
                switch (action) {
                    case "nextPage" -> songSelectionGUI.nextPage();
                    case "previousPage" -> songSelectionGUI.previousPage();
                    case "voteSkip" -> {
                        player.performCommand("bard vote");
                        player.closeInventory();
                    }
                    case "stopSong" -> {
                        if (player.hasPermission("bard.stop.any") || player.equals(songManager.getSongStarter())) {
                            if (songManager.isSongPlaying()) {
                                songManager.stopCurrentSong();
                                player.closeInventory();
                                plugin.getMessageUtil().sendParsedMessage(player, "<red>Song ended");
                            }
                        } else {
                            plugin.getMessageUtil().sendParsedMessage(player, "<red>You can only stop your own songs.");
                        }
                    }
                    default -> plugin.debugLog("Invalid action: " + action);
                }
            } else {
                plugin.debugLog("Item has no meta");
            }
        }
    }

}