package me.xidentified.tavernbard.managers;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.xidentified.tavernbard.*;
import me.xidentified.tavernbard.BardTrait;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
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
    public final Map<UUID, UUID> bardNpcs = new ConcurrentHashMap<>();

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
        UUID bardNpc = bardNpcs.get(npcId);
        if (bardNpc == null) {
            return null;
        }
        return new SongSelectionGUI(plugin, plugin.getSongManager(), bardNpc);
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

    public void playSongForNearbyPlayers(@NotNull Player player, @NotNull UUID npcId, @NotNull Song selectedSong, boolean chargePlayer) {
        MessageUtil messageUtil = this.plugin.getMessageUtil();
        CooldownManager cooldownManager = this.plugin.getCooldownManager();
        UUID bardNpc = bardNpcs.get(npcId);
        Object message = selectedSong.getDisplayName();

        if (bardNpc == null) {
            plugin.getLogger().severe("Could not retrieve NPC for ID: " + npcId);
            return;
        }

        plugin.debugLog("Attempting to play song: " + message + " for " + songStarter.get(player.getUniqueId()));

        // Check if item cost is enabled, return if they can't afford it
        if (!cooldownManager.isOnCooldown(player) && chargePlayer && itemCostManager.isEnabled() && !itemCostManager.canAfford(player)) {
            player.sendMessage(plugin.getMessageUtil().convertToUniversalFormat("<red>You need " + itemCostManager.getCostAmount() + " " + itemCostManager.formatEnumName(itemCostManager.getCostItem().name()) + "(s) to play a song!"));
            return;
        }
        // Check if economy is enabled
        if(!cooldownManager.isOnCooldown(player) && chargePlayer && plugin.getConfig().getBoolean("economy.enabled")) {
            double costPerSong = plugin.getConfig().getDouble("economy.cost-per-song");

            // Check and charge the player
            if(!economyManager.chargePlayer(player, costPerSong)) {
                player.sendMessage(plugin.getMessageUtil().convertToUniversalFormat("<red>You need " + costPerSong + " coins to play a song!"));
                return;
            } else {
                player.sendMessage(plugin.getMessageUtil().convertToUniversalFormat("<green>Paid " + costPerSong + " coins to play a song!"));
            }
        }
        if (!cooldownManager.isOnCooldown(player) && chargePlayer && itemCostManager.isEnabled()) {
            itemCostManager.deductCost(player);
            player.sendMessage(plugin.getMessageUtil().convertToUniversalFormat("<green>Charged " + itemCostManager.getCostAmount() + " " + itemCostManager.formatEnumName(itemCostManager.getCostItem().name()) + "(s) to play a song!"));
        }

        // If something is already playing, add song to queue
        if (isSongPlaying(npcId)) {
            queueManager.addSongToQueue(npcId, selectedSong, player);
            return;
        }

        setSongPlaying(npcId, true);
        Location bardLocation = Objects.requireNonNull(plugin.getEntityFromUUID(player.getWorld(), bardNpc)).getLocation();
        plugin.debugLog("Playing sound reference: " + selectedSong.getSoundReference());

        // Play song and show title to players within bard's radius
        for (Player nearbyPlayer : bardLocation.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(bardLocation) <= songPlayRadius) {
                nearbyPlayer.playSound(bardLocation, selectedSong.getSoundReference(), 1.0F, 1.0F);

                // Convert display name to a universally formatted string
                String universalFormattedString = messageUtil.convertToUniversalFormat(selectedSong.getDisplayName());

                if (messageUtil.isPaper()) {
                    // If Paper, parse the string back to a Component for displaying
                    Component parsedDisplayNameComponent = messageUtil.parse(universalFormattedString);
                    Component mainTitle = Component.text("");
                    Component subtitle = Component.text("Now playing: ", NamedTextColor.YELLOW)
                            .append(parsedDisplayNameComponent);

                    Title title = Title.title(mainTitle, subtitle);
                    nearbyPlayer.showTitle(title);
                } else {
                    // If Spigot, use the universally formatted string directly
                    nearbyPlayer.sendTitle("", "Now playing: " + universalFormattedString, 10, 70, 20);
                }
            }

            // Set current song
            currentSong.put(npcId, new Song(selectedSong.getNamespace(), selectedSong.getName(), selectedSong.getDisplayName(), selectedSong.getArtist(), selectedSong.getDuration(), songStarter.get(npcId).getUniqueId()));

            // Update now playing info
            SongSelectionGUI gui = getSongSelectionGUIForNPC(npcId);
            if (gui != null) {
                gui.updateNowPlayingInfo();
            }
        }

        plugin.debugLog("Sound play attempt completed.");

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Entity bardEntity = plugin.getEntityFromUUID(player.getWorld(), npcId);
            if (bardEntity != null) {
                Location particleLocation = bardEntity.getLocation().add(0, 2.5, 0);
                bardEntity.getWorld().spawnParticle(Particle.NOTE, particleLocation, 1);
            } else {
                plugin.debugLog("Entity with UUID " + npcId + " is null when trying to spawn particles.");
            }
        }, 0L, 20L).getTaskId();

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

            UUID bardNpc = bardNpcs.get(npcId);
            if (bardNpc != null) {
                World world = Bukkit.getServer().getWorlds().get(0);
                Entity entity = plugin.getEntityFromUUID(world, bardNpc);
                if (entity instanceof Player bardPlayer) {
                    for (Player nearbyPlayer : bardPlayer.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(bardPlayer.getLocation()) <= songPlayRadius) {
                            nearbyPlayer.stopAllSounds();
                        }
                    }
                }

                SongSelectionGUI gui = getSongSelectionGUIForNPC(npcId);
                if (gui != null) {
                    gui.updateNowPlayingInfo();
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

    public Player getSongStarter(UUID playerId) {
        return songStarter.get(playerId);
    }

    public Song getCurrentSong(UUID npcId) {
        return currentSong.get(npcId);
    }


    public UUID getNearestBard(Player player, double searchRadius) {
        double closestDistanceSquared = searchRadius * searchRadius;
        UUID closestBard = null;

        // Get all entities within the search radius
        List<Entity> nearbyEntities = player.getNearbyEntities(searchRadius, searchRadius, searchRadius);

        for (Entity entity : nearbyEntities) {
            // Check for Citizens NPC
            if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
                if (!npc.hasTrait(BardTrait.class)) {
                    continue;
                }
            }
            // Check for MythicMob
            else if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
                if (!MythicBukkit.inst().getAPIHelper().isMythicMob(entity)) {
                    continue;
                }
                ActiveMob activeMob = MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entity);
                if (!activeMob.getType().getConfig().getBoolean("Options.IsBard")) {
                    continue;
                }
            }
            // If not an NPC or MythicMob, skip this entity
            else continue;

            // Calculate squared distance
            double distanceSquared = entity.getLocation().distanceSquared(player.getLocation());

            // Update if another entity is closer
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closestBard = entity.getUniqueId();
            }
        }

        return closestBard;
    }

}