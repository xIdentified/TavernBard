package me.xidentified.tavernbard;

import me.xidentified.tavernbard.listeners.BardInteractListener;
import me.xidentified.tavernbard.listeners.GUIListener;
import me.xidentified.tavernbard.listeners.ResourcePackListener;
import me.xidentified.tavernbard.managers.CooldownManager;
import me.xidentified.tavernbard.managers.QueueManager;
import me.xidentified.tavernbard.managers.SongManager;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class TavernBard extends JavaPlugin {

    private SongManager songManager;
    private MessageUtil messageUtil;
    private CooldownManager cooldownManager;
    private BardInteractListener bardInteractListener;
    private boolean mythicMobsEnabled = false;

    @Override
    public void onEnable() {
        // Check if Citizens is enabled
        if (getServer().getPluginManager().getPlugin("Citizens") == null ||
                !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
            getLogger().log(Level.SEVERE, "Citizens not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check if MythicMobs is enabled
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            getLogger().info("MythicMobs found! Enabling support...");
            mythicMobsEnabled = true;
        } else {
            getLogger().info("ModelEngine not found. Running without ModelEngine support.");
        }

        // Load default config if missing and saved plugin configuration
        saveDefaultConfig();
        reloadConfig();

        // Initialize SongManager, QueueManager, and MessageUtil classes
        this.messageUtil = new MessageUtil(getConfig());
        cooldownManager = new CooldownManager();
        songManager = new SongManager(this);
        bardInteractListener = new BardInteractListener(this);
        QueueManager queueManager = new QueueManager(this, songManager, cooldownManager);

        // Register the bard trait with Citizens.
        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class));

        // Initialize and register listeners
        GUIListener GUIListener = new GUIListener(this, songManager);
        getServer().getPluginManager().registerEvents(GUIListener, this);

        ResourcePackListener resourcePackListener = new ResourcePackListener(this);
        getServer().getPluginManager().registerEvents(resourcePackListener, this);

        getServer().getPluginManager().registerEvents(bardInteractListener, this);

        // Register commands
        Objects.requireNonNull(this.getCommand("bard")).setExecutor(new CommandHandler(songManager, queueManager));

    }

    // Get entity from UUID to check if it's a bard or not
    public Entity getEntityFromUUID(UUID entityId) {
        for (World world : getServer().getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    // Plugin shutdown logic
    @Override
    public void onDisable() {
        // Unregister the bard trait
        if (CitizensAPI.hasImplementation()) {
            CitizensAPI.getTraitFactory().deregisterTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class));
        }

        // Unregister listeners
        HandlerList.unregisterAll();

        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);

    }

    // Getter methods
    public SongManager getSongManager() {
        return songManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public BardInteractListener getBardInteractListener() {
        return bardInteractListener;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public boolean isMythicMobsEnabled() {
        return mythicMobsEnabled;
    }

}
