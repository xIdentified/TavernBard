package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.*;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SongManager {

    private final TavernBard plugin;
    private final QueueManager queueManager;
    private final EconomyManager economyManager;
    private final ItemCostManager itemCostManager;
    private final List<Song> songs;
    protected final double songPlayRadius;
    private final int defaultSongDuration;
    public final Map<UUID, Player> songStarter = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isSongPlaying = new ConcurrentHashMap<>();
    private final Map<UUID, Song> currentSong = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> currentSongTaskId = new ConcurrentHashMap<>();
    public final Map<UUID, NPC> bardNpcs = new ConcurrentHashMap<>();

    public SongManager(TavernBard plugin) {
        this.plugin = plugin;
        this.queueManager = new QueueManager(this.plugin, this, plugin.getCooldownManager());
        this.economyManager = new EconomyManager(this.plugin);
        this.songs = loadSongsFromConfig();
        this.songPlayRadius = plugin.getConfig().getDouble("song-play-radius", 20.0);
        this.defaultSongDuration = plugin.getConfig().getInt("default-song-duration", 180);
        this.itemCostManager = new ItemCostManager(
                plugin.getConfig().getString("item-cost.item", "GOLD_NUGGET"),
                plugin.getConfig().getInt("item-cost.amount", 3),
                plugin.getConfig().getBoolean("item-cost.enabled", false),
                plugin
        );
    }

    public SongSelectionGUI getSongSelectionGUIForNPC(UUID npcId) {
        NPC bardNpc = bardNpcs.get(npcId);
        if (bardNpc == null) {
            return null;
        }
        return new SongSelectionGUI(plugin, plugin.getSongManager(), bardNpc.getUniqueId());
    }

    // Reload songs from config
    public void reloadSongs() {
        plugin.reloadConfig();
        songs.clear();
        songs.addAll(loadSongsFromConfig());
    }

    // Load songs from config
    private List<Song> loadSongsFromConfig() {
        List<Song> loadedSongs = new ArrayList<>();
        FileConfiguration config = plugin.getConfig();

        if (config.isConfigurationSection("songs")) {
            ConfigurationSection songsSection = config.getConfigurationSection("songs");
            assert songsSection != null;
            for (String key : songsSection.getKeys(false)) {
                String namespace = songsSection.getString(key + ".namespace");
                String songName = songsSection.getString(key + ".name", key);
                String displayName = songsSection.getString(key + ".displayName", songName);
                String artist = songsSection.getString(key + ".artist", "Unknown Artist");
                int duration = songsSection.getInt(key + ".duration", defaultSongDuration);
                loadedSongs.add(new Song(namespace, songName, displayName, artist, duration));
            }
        } else {
            plugin.debugLog("The 'songs' section is missing or misconfigured in config.yml!");
        }
        return loadedSongs;
    }

    public void playSongForNearbyPlayers(Player player, @NotNull UUID npcId, @NotNull Song selectedSong, boolean chargePlayer) {

        Player starter = songStarter.get(npcId);
        if(starter == null) {
            plugin.getLogger().severe("songStarter does not contain key: " + npcId);
            return;
        }

        NPC bardNpc = bardNpcs.get(npcId);
        if (bardNpc == null) {
            // Log or handle the error accordingly
            plugin.getLogger().severe("Could not retrieve NPC for ID: " + npcId);
            return;
        }

        plugin.debugLog("Attempting to play song: " + selectedSong.getDisplayName() + " for " + songStarter);
        MessageUtil messageUtil = this.plugin.getMessageUtil();
        CooldownManager cooldownManager = this.plugin.getCooldownManager();

        // Check if item cost is enabled, return if they can't afford it
        if (!cooldownManager.isOnCooldown(player) && chargePlayer && itemCostManager.isEnabled() && !itemCostManager.canAfford(player)) {
            messageUtil.sendParsedMessage(player, "<red>You need " + itemCostManager.getCostAmount() + " " + itemCostManager.formatEnumName(itemCostManager.getCostItem().name()) + "(s) to play a song!");
            return;
        }

        // Check if economy is enabled
        if(!cooldownManager.isOnCooldown(player) && chargePlayer && plugin.getConfig().getBoolean("economy.enabled")) {
            double costPerSong = plugin.getConfig().getDouble("economy.cost-per-song");

            // Check and charge the player
            if(!economyManager.chargePlayer(player, costPerSong)) {
                messageUtil.sendParsedMessage(player, "<red>You need " + costPerSong + " coins to play a song!");
                return;
            } else {
                messageUtil.sendParsedMessage(player, "<green>Paid " + costPerSong + " coins to play a song!");
            }
        }

        if (!cooldownManager.isOnCooldown(player) && chargePlayer && itemCostManager.isEnabled()) {
            itemCostManager.deductCost(player);
            messageUtil.sendParsedMessage(player, "<green>Charged " + itemCostManager.getCostAmount() + " " + itemCostManager.formatEnumName(itemCostManager.getCostItem().name()) + "(s) to play a song!");
        }

        // If something is already playing, add song to queue
        if (isSongPlaying(npcId)) {
            queueManager.addSongToQueue(npcId, selectedSong, player);
            return;
        }

        setSongPlaying(npcId, true);
        Location bardLocation = bardNpc.getEntity().getLocation();
        plugin.debugLog("Playing sound reference: " + selectedSong.getSoundReference());

        // Play song and show title to players within bard's radius
        for (Player nearbyPlayer : bardLocation.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(bardLocation) <= songPlayRadius) {
                nearbyPlayer.playSound(bardLocation, selectedSong.getSoundReference(), 1.0F, 1.0F);

                // Parse song display name and artist
                var mm = MiniMessage.miniMessage();
                Component parsedDisplayNameComponent = mm.deserialize(selectedSong.getDisplayName());

                // Sending the title
                Component mainTitle = Component.text("");
                Component subtitle = Component.text("Now playing: ", NamedTextColor.GOLD)
                        .append(parsedDisplayNameComponent);

                Title title = Title.title(mainTitle, subtitle);
                nearbyPlayer.showTitle(title);
            }
            currentSong.put(npcId, new Song(selectedSong.getNamespace(), selectedSong.getName(), selectedSong.getDisplayName(), selectedSong.getArtist(), selectedSong.getDuration(), songStarter.get(npcId).getUniqueId()));

            // Update now playing info
            SongSelectionGUI gui = getSongSelectionGUIForNPC(npcId);
            if (gui != null) {
                gui.updateNowPlayingInfo();
            }
        }

        plugin.debugLog("Sound play attempt complete");

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> bardNpc.getEntity().getWorld().spawnParticle(Particle.NOTE, bardNpc.getEntity().getLocation().add(0, 2.5, 0), 1),
                0L, 20L ).getTaskId();

        long songDurationInTicks = selectedSong.getDuration() * 20L;
        currentSongTaskId.put(npcId, taskId);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.debugLog("Song ended. Attempting to play next song in the queue.");
            Bukkit.getScheduler().cancelTask(taskId);
            setSongPlaying(npcId, false);
            playNextSong(npcId, player);
        }, songDurationInTicks);
    }

    // Stops the current song for nearby players
    public void stopCurrentSong(UUID npcId) {
        if (isSongPlaying(npcId) && currentSongTaskId.containsKey(npcId) && currentSongTaskId.get(npcId) != -1) {
            Bukkit.getScheduler().cancelTask(currentSongTaskId.get(npcId));
            setSongPlaying(npcId, false);

            NPC bardNpc = bardNpcs.get(npcId);
            if (bardNpc != null) {
                for (Player nearbyPlayer : bardNpc.getEntity().getLocation().getWorld().getPlayers()) {
                    if (nearbyPlayer.getLocation().distance(bardNpc.getEntity().getLocation()) <= songPlayRadius) {
                        nearbyPlayer.stopAllSounds();
                    }
                    // Assuming you'll adapt songSelectionGUI to be NPC-specific
                    SongSelectionGUI gui = getSongSelectionGUIForNPC(npcId);
                    if (gui != null) {
                        gui.updateNowPlayingInfo();
                    }
                }
            }
            currentSong.remove(npcId);
            playNextSong(npcId, songStarter.get(npcId));
            songStarter.remove(npcId);
        }
    }

    public void playNextSong(UUID npcId, Player songStarter) {
        // Attempt to play next song in queue
        Song nextSong = queueManager.getNextSongFromQueue(npcId);
        if (nextSong != null && npcId != null) {
            playSongForNearbyPlayers(songStarter, npcId, nextSong, false);
        }
    }

    public boolean isSongPlaying(UUID npcId) {
        return isSongPlaying.getOrDefault(npcId, false);
    }

    private void setSongPlaying(UUID npcId, boolean status) {
        isSongPlaying.put(npcId, status);
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public Song getSongByName(String actualSongName) {
        return songs.stream()
                .filter(song -> song.getName().equalsIgnoreCase(actualSongName))
                .findFirst()
                .orElse(null);
    }

    public Player getSongStarter(UUID npcId) {
        return songStarter.get(npcId);
    }

    public Song getCurrentSong(UUID npcId) {
        return currentSong.get(npcId);
    }

    public NPC getBardNpc(UUID npcId) {
        return bardNpcs.get(npcId);
    }

    public UUID getNearestBardNpc(Player player) {
        double closestDistanceSquared = Double.MAX_VALUE;
        NPC closestNpc = null;

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            // Check if the NPC has the bard trait
            if (!npc.hasTrait(BardTrait.class)) {
                continue;
            }

            // Calculate the squared distance to avoid unnecessary sqrt calculations
            double distanceSquared = npc.getEntity().getLocation().distanceSquared(player.getLocation());

            // Update closest NPC if this one is nearer
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestNpc = npc;
            }
        }

        // If no bard NPCs were found, return null
        if (closestNpc == null) {
            return null;
        }

        // Otherwise, return the UUID of the closest bard NPC
        return closestNpc.getUniqueId();
    }

}