package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.Song;
import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {
    private final SongManager songManager;
    private final MessageUtil messageUtil;
    protected final Queue<Song> songQueue = new LinkedList<>();
    private final int MAX_QUEUE_SIZE;
    private final Set<UUID> playersVotedToSkip = new HashSet<>();
    private final TavernBard plugin;
    private int skipVotesCount = 0;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>(); // cooldown for adding songs to queue
    private final long EIGHT_HOURS_IN_MILLIS = 8 * 60 * 60 * 1000; // 8hrs in milliseconds

    public QueueManager(TavernBard plugin, SongManager songManager) {
        this.plugin = plugin;
        this.songManager = songManager;
        this.messageUtil = plugin.getMessageUtil();
        this.MAX_QUEUE_SIZE = plugin.getConfig().getInt("max-queue-size", 10);
        long COOLDOWN_CLEANUP_PERIOD = 36000L; // 30 mins
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldCooldowns, COOLDOWN_CLEANUP_PERIOD, COOLDOWN_CLEANUP_PERIOD);
    }

    public void addSongToQueue(Song song, Player player) {
        plugin.debugLog("Attempting to add song to queue: " + song.getDisplayName() + " by " + (player != null ? player.getName() : "NULL"));

        if (isOnCooldown(player)) {
            plugin.debugLog("Player is on cooldown. Cannot add song to queue: " + song.getDisplayName() + " by " + player.getName());
            messageUtil.sendParsedMessage(player, "<red>You need to wait before queueing another song.");
            return;
        }

        if (songQueue.size() >= MAX_QUEUE_SIZE) {
            messageUtil.sendParsedMessage(player, "<red>The queue is full! Please wait for a few songs to finish.");
            return;
        }

        player.sendMessage("§aThe song has been added to the queue.");
        songQueue.add(new Song(song.getNamespace(), song.getName(), song.getDisplayName(), song.getArtist(), song.getDuration(), player.getUniqueId()));
        updateCooldown(player);
        plugin.debugLog("Last song added to queue by: " + (song.getAddedByName() != null ? song.getAddedByName() : "NULL"));
    }

    public Song getNextSongFromQueue() {
        return songQueue.poll();
    }

    public Queue<Song> getQueueStatus() {
        return new LinkedList<>(songQueue);
    }

    public void voteToSkip(Player player) {

        if (playersVotedToSkip.contains(player.getUniqueId())) {
            messageUtil.sendParsedMessage(player, "<red>You have already voted to skip this song.");
            return;
        }

        playersVotedToSkip.add(player.getUniqueId());
        skipVotesCount++;

        int nearbyPlayersCount = (int) songManager.bardNpc.getEntity().getLocation().getWorld().getPlayers().stream()
                .filter(nearbyPlayer -> nearbyPlayer.getLocation().distance(songManager.bardNpc.getEntity().getLocation()) <= songManager.songPlayRadius)
                .count();

        if (songManager.isSongPlaying() && skipVotesCount > nearbyPlayersCount / 2) {
            songManager.stopCurrentSong();
            resetSkipVotes();
            Song nextSong = getNextSongFromQueue();
            if (nextSong != null) {
                songManager.playSongForNearbyPlayers(songManager.songStarter, songManager.bardNpc, nextSong, true);
            }
            messageUtil.sendParsedMessage(player, "<red>The song has been skipped due to majority vote.");
        } else {
            messageUtil.sendParsedMessage(player, "<green>You have voted to skip the current song.");
        }
    }

    // When a song ends or is skipped, call to reset queue votes
    private void resetSkipVotes() {
        playersVotedToSkip.clear();
        skipVotesCount = 0;
    }


    //Handle cooldowns for adding songs to queue
    public boolean isOnCooldown(Player player) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }

        long lastAdded = playerCooldowns.get(player.getUniqueId());
        long currentTimestamp = System.currentTimeMillis();

        long COOLDOWN_DURATION = 60000;
        boolean onCooldown = (currentTimestamp - lastAdded) < COOLDOWN_DURATION;

        plugin.debugLog("Player: " + player.getName() + " Last Added: " + lastAdded + " Current Time: " + currentTimestamp + " On Cooldown: " + onCooldown);

        return onCooldown;
    }

    public void updateCooldown(Player player) {
        plugin.debugLog("Updating cooldown for player: " + player.getName());
        playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void cleanupOldCooldowns() {
        long currentTimestamp = System.currentTimeMillis();
        playerCooldowns.entrySet().removeIf(entry -> currentTimestamp - entry.getValue() > EIGHT_HOURS_IN_MILLIS);
    }

}
