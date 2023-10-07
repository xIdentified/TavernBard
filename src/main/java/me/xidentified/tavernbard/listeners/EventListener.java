package me.xidentified.tavernbard.listeners;

import me.xidentified.tavernbard.Song;
import me.xidentified.tavernbard.SongSelectionGUI;
import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.managers.SongManager;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EventListener implements Listener {
    private final SongManager songManager;
    private final TavernBard plugin;
    private final MessageUtil messageUtil;

    public EventListener(TavernBard plugin, SongManager songManager, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.songManager = songManager;
        this.messageUtil = messageUtil;
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
                                messageUtil.sendParsedMessage(player, "<red>Song ended");
                            }
                        } else {
                            messageUtil.sendParsedMessage(player, "<red>You can only stop your own songs.");
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