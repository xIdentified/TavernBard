package me.xidentified.tavernbard.managers;

import me.xidentified.tavernbard.Song;
import me.xidentified.tavernbard.TavernBard;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class QueueManager {
    private final SongManager songManager;
    private final MessageUtil messageUtil;
    private final Map<UUID, Queue<Song>> npcSongQueues = new HashMap<>();
    private final Map<UUID, Set<UUID>> npcPlayersVotedToSkip = new HashMap<>();
    private final Map<UUID, Integer> npcSkipVotesCount = new HashMap<>();
    private final TavernBard plugin;
    private final int MAX_QUEUE_SIZE;

    public QueueManager(TavernBard plugin, SongManager songManager) {
        this.plugin = plugin;
        this.songManager = songManager;
        this.messageUtil = plugin.getMessageUtil();
        this.MAX_QUEUE_SIZE = plugin.getConfig().getInt("max-queue-size", 10);
    }

    public void addSongToQueue(UUID npcId, Song song, @NotNull Player player) {
        npcSongQueues.computeIfAbsent(npcId, k -> new LinkedList<>());

        if (npcSongQueues.size() >= MAX_QUEUE_SIZE) {
            messageUtil.sendParsedMessage(player, "<red>The queue is full! Please wait for a few songs to finish.");
            return;
        }

        player.sendMessage("§aThe song has been added to the queue.");
        npcSongQueues.get(npcId).add(new Song(song.getNamespace(), song.getName(), song.getDisplayName(), song.getArtist(), song.getDuration(), player.getUniqueId()));
        plugin.debugLog("Last song added to queue by: " + (song.getAddedByName() != null ? song.getAddedByName() : "NULL"));
    }

    public Song getNextSongFromQueue(UUID npcId) {
        Queue<Song> queue = npcSongQueues.get(npcId);
        return (queue != null) ? queue.poll() : null;
    }

    public Queue<Song> getQueueStatus(UUID npcId) {
        Queue<Song> queue = npcSongQueues.get(npcId);
        return (queue != null) ? new LinkedList<>(queue) : new LinkedList<>();
    }

    public void voteToSkip(Player player, UUID npcId) {
        // Ensure NPC has skip vote data
        npcPlayersVotedToSkip.computeIfAbsent(npcId, k -> new HashSet<>());
        npcSkipVotesCount.computeIfAbsent(npcId, k -> 0);

        // Retrieve NPC-specific skip votes and count
        Set<UUID> playersVotedToSkip = npcPlayersVotedToSkip.get(npcId);
        int skipVotesCount = npcSkipVotesCount.get(npcId);

        // Check if the player has already voted to skip
        if (playersVotedToSkip.contains(player.getUniqueId())) {
            messageUtil.sendParsedMessage(player, "<red>You have already voted to skip this song.");
            return;
        }

        // Add the player's vote to skip
        playersVotedToSkip.add(player.getUniqueId());
        npcSkipVotesCount.put(npcId, ++skipVotesCount);

        NPC bardNpc = songManager.getBardNpc(npcId); // Retrieve the NPC using npcId
        if (bardNpc == null) {
            messageUtil.sendParsedMessage(player, "<red>Error: NPC not found.");
            return;
        }

        // Calculate the number of nearby players to the NPC
        int nearbyPlayersCount = (int) bardNpc.getEntity().getLocation().getWorld().getPlayers().stream()
                .filter(nearbyPlayer -> nearbyPlayer.getLocation().distance(bardNpc.getEntity().getLocation()) <= songManager.songPlayRadius)
                .count();

        // Check if the song should be skipped based on the majority vote
        if (songManager.isSongPlaying(npcId) && skipVotesCount > nearbyPlayersCount / 2) {
            songManager.stopCurrentSong(npcId);
            resetSkipVotes(npcId);
            Song nextSong = getNextSongFromQueue(npcId);
            if (nextSong != null) {
                Player songStarter = songManager.getSongStarter(npcId); // Retrieve the player who started the song using npcId
                if (songStarter != null) {
                    songManager.playSongForNearbyPlayers(songStarter, npcId, nextSong, true);
                }
            }
            messageUtil.sendParsedMessage(player, "<red>The song has been skipped due to majority vote.");
        } else {
            messageUtil.sendParsedMessage(player, "<green>You have voted to skip the current song.");
        }
    }


    // When a song ends or is skipped, call to reset queue votes
    private void resetSkipVotes(UUID npcId) {
        Set<UUID> playersVotedToSkip = npcPlayersVotedToSkip.get(npcId);
        if (playersVotedToSkip != null) playersVotedToSkip.clear();
        npcSkipVotesCount.put(npcId, 0);
    }

}
