package me.xidentified.tavernbard;

import me.xidentified.tavernbard.managers.QueueManager;
import me.xidentified.tavernbard.managers.SongManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.UUID;

public class CommandHandler implements CommandExecutor {
    private final SongManager songManager;
    private final QueueManager queueManager;

    public CommandHandler(SongManager songManager, QueueManager queueManager) {
        this.songManager = songManager;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use bard commands.");
            return true;
        }

        // Assume a method getNearestBardNpc which returns the closest Bard NPC to a player
        UUID npcId = songManager.getNearestBard(player, 8);

        if (npcId == null) {
            sender.sendMessage("§cNo nearby bard NPCs found!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("bard") && args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("bard.reload")) {
                    songManager.reloadSongs();
                    sender.sendMessage("§aTavernBard configuration successfully reloaded!");
                } else {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("queue")) {
                if (sender.hasPermission("bard.play")) {
                    Queue<Song> queue = queueManager.getQueueStatus(npcId); // Updated to use NPC ID
                    if (queue.isEmpty()) {
                        sender.sendMessage("§cThere are no songs in the queue.");
                        return true;
                    }
                    sender.sendMessage("§aUpcoming songs:");
                    for (Song song : queue) {
                        sender.sendMessage("§6" + song.getDisplayName() + "§7 added by §e" + song.getAddedByName());
                    }
                } else {
                    sender.sendMessage("§cYou don't have permission to view the queue.");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("vote")) {
                if (!songManager.isSongPlaying(npcId)) {
                    sender.sendMessage("§cThere are no songs playing to vote against.");
                    return true;
                }
                queueManager.voteToSkip(player, npcId);
                return true;
            }
        }
        return false;
    }
}

