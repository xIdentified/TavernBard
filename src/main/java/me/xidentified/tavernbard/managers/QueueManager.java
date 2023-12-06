package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.Song;
import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.util.MessageUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Bukkit.getEntity;

public class QueueManager {
    private final SongManager songManager;
    private final MessageUtil messageUtil;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Queue<Song>> npcSongQueues = new HashMap<>();
    private final Map<UUID, Set<UUID>> npcPlayersVotedToSkip = new HashMap<>();
    private final Map<UUID, Integer> npcSkipVotesCount = new HashMap<>();
    private final TavernBard plugin;
    private final int MAX_QUEUE_SIZE;

    public QueueManager(TavernBard plugin, SongManager songManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.songManager = songManager;
        this.cooldownManager = cooldownManager;
        this.messageUtil = plugin.getMessageUtil();
        this.MAX_QUEUE_SIZE = plugin.getConfig().getInt("max-queue-size", 10);
    }

    public void addSongToQueue(UUID bardEntityId, Song song, @NotNull Player player) {
        npcSongQueues.computeIfAbsent(bardEntityId, k -> new LinkedList<>());

        if (cooldownManager.isOnCooldown(player)) {
            long timeLeft = TimeUnit.MILLISECONDS.toSeconds(cooldownManager.getTimeLeft(player));
            messageUtil.sendParsedMessage(player, "<red>You can't add another song for " + timeLeft + " seconds!");
            return;
        }

        if (npcSongQueues.size() >= MAX_QUEUE_SIZE) {
            messageUtil.sendParsedMessage(player, "<red>The queue is full! Please wait for a few songs to finish.");
            return;
        }

        player.sendMessage("§aThe song has been added to the queue.");
        npcSongQueues.get(bardEntityId).add(new Song(song.getNamespace(), song.getName(), song.getDisplayName(), song.getArtist(), song.getDuration(), player.getUniqueId()));
        cooldownManager.setCooldown(player);
        plugin.debugLog("Last song added to queue by: " + (song.getAddedByName() != null ? song.getAddedByName() : "NULL"));
    }

    public Song getNextSongFromQueue(UUID bardEntityId) {
        Queue<Song> queue = npcSongQueues.get(bardEntityId);
        return (queue != null) ? queue.poll() : null;
    }

    public Queue<Song> getQueueStatus(UUID bardEntityId) {
        Queue<Song> queue = npcSongQueues.get(bardEntityId);
        return (queue != null) ? new LinkedList<>(queue) : new LinkedList<>();
    }

    public void voteToSkip(Player player, UUID bardEntityId) {
        // Ensure NPC has skip vote data
        npcPlayersVotedToSkip.computeIfAbsent(bardEntityId, k -> new HashSet<>());
        npcSkipVotesCount.putIfAbsent(bardEntityId, 0);

        // Retrieve NPC-specific skip votes and count
        Set<UUID> playersVotedToSkip = npcPlayersVotedToSkip.get(bardEntityId);
        int skipVotesCount = npcSkipVotesCount.get(bardEntityId);

        // Check if the player has already voted to skip
        if (playersVotedToSkip.contains(player.getUniqueId())) {
            messageUtil.sendParsedMessage(player, "<red>You have already voted to skip this song.");
            return;
        }

        // Add the player's vote to skip
        playersVotedToSkip.add(player.getUniqueId());
        npcSkipVotesCount.put(bardEntityId, ++skipVotesCount);

        // Calculate the number of nearby players to the NPC
        int nearbyPlayersCount = (int) plugin.getEntityFromUUID(player.getWorld(), bardEntityId).getLocation().getWorld().getPlayers().stream()
                .filter(nearbyPlayer -> nearbyPlayer.getLocation().distance(plugin.getEntityFromUUID(player.getWorld(), bardEntityId).getLocation()) <= songManager.songPlayRadius)
                .count();

        // Check if the song should be skipped based on the majority vote
        if (songManager.isSongPlaying(bardEntityId) && skipVotesCount > nearbyPlayersCount / 2) {
            songManager.stopCurrentSong(bardEntityId);
            resetSkipVotes(bardEntityId);
            Song nextSong = getNextSongFromQueue(bardEntityId);
            if (nextSong != null) {
                Player songStarter = songManager.getSongStarter(bardEntityId);
                if (songStarter != null) {
                    songManager.playSongForNearbyPlayers(songStarter, bardEntityId, nextSong, true);
                }
            }
            messageUtil.sendParsedMessage(player, "<red>The song has been skipped due to majority vote.");
        } else {
            messageUtil.sendParsedMessage(player, "<green>You have voted to skip the current song.");
        }
    }


    // When a song ends or is skipped, call to reset queue votes
    private void resetSkipVotes(UUID bardEntityId) {
        Set<UUID> playersVotedToSkip = npcPlayersVotedToSkip.get(bardEntityId);
        if (playersVotedToSkip != null) playersVotedToSkip.clear();
        npcSkipVotesCount.put(bardEntityId, 0);
    }

}
