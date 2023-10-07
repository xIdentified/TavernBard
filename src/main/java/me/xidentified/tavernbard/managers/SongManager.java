package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.*;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class SongManager {

    private final TavernBard plugin;
    private final QueueManager queueManager;
    private final EconomyManager economyManager;
    private final SongSelectionGUI songSelectionGUI;
    private final List<Song> songs;
    private boolean isSongPlaying = false;
    protected final double songPlayRadius;
    private final int defaultSongDuration;
    protected Player songStarter = null;
    private Song currentSong;
    private int currentSongTaskId = -1;
    protected NPC bardNpc;

    public SongManager(TavernBard plugin) {
        this.plugin = plugin;
        this.queueManager = new QueueManager(this.plugin, this);
        this.economyManager = new EconomyManager(this.plugin);
        this.songs = loadSongsFromConfig();
        this.songPlayRadius = plugin.getConfig().getDouble("song-play-radius", 20.0);
        this.defaultSongDuration = plugin.getConfig().getInt("default-song-duration", 180);
        this.songSelectionGUI = new SongSelectionGUI(this.plugin, this, bardNpc, this.plugin.getMessageUtil());
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


    public void playSongForNearbyPlayers(Player player, NPC bardNpc, Song selectedSong) {
        songStarter = player;
        this.bardNpc = bardNpc;
        plugin.debugLog("Attempting to play song: " + selectedSong.getDisplayName() + " for " + (songStarter != null ? songStarter.getName() : "Unknown Player"));

        // Check if economy is enabled
        if(plugin.getConfig().getBoolean("economy.enabled")) {
            double costPerSong = plugin.getConfig().getDouble("economy.cost-per-song");

            // Check and charge the player
            if(!economyManager.chargePlayer(player, costPerSong)) {
                MessageUtil messageUtil = this.plugin.getMessageUtil();
                messageUtil.sendParsedMessage(player, "<red>You do not have enough money to play a song!");
                return;
            }
        }

        // If something is already playing, add song to queue
        if (isSongPlaying()) {
            queueManager.addSongToQueue(selectedSong, player);
            return;
        }

        setSongPlaying(true);
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
            currentSong = new Song(selectedSong.getNamespace(), selectedSong.getName(), selectedSong.getDisplayName(), selectedSong.getArtist(), selectedSong.getDuration(), songStarter.getUniqueId());
            songSelectionGUI.updateNowPlayingInfo();
        }


        plugin.debugLog("Sound play attempt complete");

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> bardNpc.getEntity().getWorld().spawnParticle(Particle.NOTE, bardNpc.getEntity().getLocation().add(0, 2.5, 0), 1),
                0L, 20L ).getTaskId();

        long songDurationInTicks = selectedSong.getDuration() * 20L;
        currentSongTaskId = taskId;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.debugLog("Song ended. Attempting to play next song in the queue.");
            Bukkit.getScheduler().cancelTask(taskId);
            setSongPlaying(false);
            playNextSong(player);
        }, songDurationInTicks);
    }

    // Stops the current song for nearby players
    public void stopCurrentSong() {
        if (isSongPlaying() && currentSongTaskId != -1) {
            Bukkit.getScheduler().cancelTask(currentSongTaskId);
            setSongPlaying(false);

            // Gather players around NPC radius
            for (Player nearbyPlayer : bardNpc.getEntity().getLocation().getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(bardNpc.getEntity().getLocation()) <= songPlayRadius) {
                    // Stop any sounds that are playing nearby
                    nearbyPlayer.stopAllSounds();
                }
                songSelectionGUI.updateNowPlayingInfo();
                currentSong = null;
            }
            playNextSong(songStarter);
            songStarter = null;
        }
    }

    public void playNextSong(Player songStarter) {
        // Attempt to play next song in queue
        Song nextSong = queueManager.getNextSongFromQueue();
        if (nextSong != null) {
            playSongForNearbyPlayers(songStarter, bardNpc, nextSong);
        }
    }

    public boolean isSongPlaying() {
        return isSongPlaying;
    }

    public void setSongPlaying(boolean songPlaying) {
        isSongPlaying = songPlaying;
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

    public Player getSongStarter() {
        return songStarter;
    }

    public Song getCurrentSong() {
        return currentSong;
    }
}