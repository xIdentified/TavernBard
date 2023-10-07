package me.xidentified.tavernbard;

import me.xidentified.tavernbard.listeners.EventListener;
import me.xidentified.tavernbard.listeners.ResourcePackListener;
import me.xidentified.tavernbard.managers.QueueManager;
import me.xidentified.tavernbard.managers.SongManager;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TavernBard extends JavaPlugin {

    private SongManager songManager;
    private MessageUtil messageUtil;

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

        // Initialize SongManager, QueueManager, and MessageUtil classes
        this.messageUtil = new MessageUtil(getConfig());
        songManager = new SongManager(this);
        QueueManager queueManager = new QueueManager(this, songManager);

        // Register the bard trait with Citizens.
        CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class));

        // Initialize and register listeners
        EventListener eventListener = new EventListener(this, songManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        ResourcePackListener resourcePackListener = new ResourcePackListener(this);
        getServer().getPluginManager().registerEvents(resourcePackListener, this);

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

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

}
