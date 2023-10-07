package me.xidentified.tavernbard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

public class CommandHandler implements CommandExecutor {
    private final SongManager songManager;
    private final QueueManager queueManager;

    public CommandHandler(SongManager songManager, QueueManager queueManager) {
        this.songManager = songManager;
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
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
                    Queue<Song> queue = queueManager.getQueueStatus();
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
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can vote to skip songs.");
                    return true;
                } else if (!songManager.isSongPlaying()) {
                    sender.sendMessage("§cThere are no songs playing to vote against.");
                    return true;
                }
                queueManager.voteToSkip((Player) sender);
                return true;
            }
        }
        return false;
    }
}

