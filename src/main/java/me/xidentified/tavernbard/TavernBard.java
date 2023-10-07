package me.xidentified.tavernbard;

import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TavernBard extends JavaPlugin {

    private SongManager songManager;

    @Override
    public void onEnable() {
        // Check if Citizens is enabled
        if (getServer().getPluginManager().getPlugin("Citizens") == null ||
                !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
            getLogger().log(Level.SEVERE, "Citizens not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load default config if missing and saved plugin configuration
        saveDefaultConfig();
        reloadConfig();

        // Initialize SongManager
        songManager = new SongManager(this);
        QueueManager queueManager = new QueueManager(this, songManager);

        // Register the bard trait with Citizens.
        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class));

        // Initialize and register EventListener
        EventListener eventListener = new EventListener(this, songManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Register commands
        this.getCommand("bard").setExecutor(new CommandHandler(songManager, queueManager));
    }

    // Plugin shutdown logic
    @Override
    public void onDisable() {
        // Unregister the bard trait
        if (CitizensAPI.hasImplementation()) {
            CitizensAPI.getTraitFactory().deregisterTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class));
        }

        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);
    }

    // Getter methods
    public SongManager getSongManager() {
        return songManager;
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

}
