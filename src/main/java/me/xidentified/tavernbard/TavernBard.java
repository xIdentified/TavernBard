package me.xidentified.tavernbard;

import me.xidentified.tavernbard.listeners.CitizensInteractListener;
import me.xidentified.tavernbard.listeners.GUIListener;
import me.xidentified.tavernbard.listeners.MythicMobInteractListener;
import me.xidentified.tavernbard.listeners.ResourcePackListener;
import me.xidentified.tavernbard.managers.CooldownManager;
import me.xidentified.tavernbard.managers.QueueManager;
import me.xidentified.tavernbard.managers.SongManager;
import me.xidentified.tavernbard.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class TavernBard extends JavaPlugin {

    private SongManager songManager;
    private MessageUtil messageUtil;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        boolean citizensFound = getServer().getPluginManager().isPluginEnabled("Citizens");
        boolean mythicMobsFound = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");

        // Check if neither Citizens nor MythicMobs is enabled
        if (!citizensFound && !mythicMobsFound) {
            getLogger().log(Level.SEVERE, "Neither Citizens nor MythicMobs found. At least one is required for the plugin to work!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check if Citizens is enabled and register bard trait
        if (citizensFound) {
            // Check if trait is already registered
            if (CitizensAPI.getTraitFactory().getTrait("bard") == null) {
                CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(BardTrait.class).withName("bard"));
                getLogger().info("Bard trait registered with Citizens.");
            }
        }

        if (mythicMobsFound) {
            getServer().getPluginManager().registerEvents(new MythicMobInteractListener(this), this);
            getLogger().info("MythicMobs detected. Support enabled!");
        }

        // Load default config if missing and saved plugin configuration
        saveDefaultConfig();
        reloadConfig();

        // Initialize SongManager, QueueManager, and MessageUtil classes
        this.messageUtil = new MessageUtil(getConfig());
        cooldownManager = new CooldownManager();
        songManager = new SongManager(this);
        QueueManager queueManager = new QueueManager(this, songManager, cooldownManager);

        // Initialize and register listeners
        GUIListener GUIListener = new GUIListener(this, songManager);
        getServer().getPluginManager().registerEvents(GUIListener, this);

        ResourcePackListener resourcePackListener = new ResourcePackListener(this);
        getServer().getPluginManager().registerEvents(resourcePackListener, this);

        CitizensInteractListener citizensInteractListener = new CitizensInteractListener(this);
        getServer().getPluginManager().registerEvents(citizensInteractListener, this);

        // Register commands
        Objects.requireNonNull(this.getCommand("bard")).setExecutor(new CommandHandler(songManager, queueManager));

    }

    public void handleInteraction(UUID bardEntityId, Player player) {
        debugLog("handleInteraction method fired");

        SongSelectionGUI gui = new SongSelectionGUI(this, getSongManager(), bardEntityId);
        player.openInventory(gui.getInventory());

        if (!getSongManager().bardNpcs.containsKey(bardEntityId)) {
            debugLog("Adding entity with ID: " + bardEntityId);
            if (bardEntityId != null) {
                getSongManager().bardNpcs.put(bardEntityId, player.getUniqueId());
            }
        } else {
            debugLog("Entity with ID: " + bardEntityId + " already added.");
        }
    }

    // Get entity from UUID to check if it's a bard or not
    public Entity getEntityFromUUID(World world, UUID uuid) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(uuid)) {
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

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void debugLog(String message) {
        if (getConfig().getBoolean("debug_mode", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

}
