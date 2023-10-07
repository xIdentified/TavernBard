package me.xidentified.tavernbard;

import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SongSelectionGUI implements InventoryHolder {

    private final TavernBard plugin;
    private final SongManager songManager;
    private final NPC bardNpc;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    private final int ITEMS_PER_PAGE = 45;
    private int currentPage = 0;
    private final Inventory cachedGUI;

    public SongSelectionGUI(TavernBard plugin, SongManager songManager, NPC bardNpc) {
        this.plugin = plugin;
        this.songManager = songManager;
        this.bardNpc = bardNpc;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();

        // Initialize the cached GUI
        this.cachedGUI = Bukkit.getServer().createInventory(this, getInventorySize(songManager.getSongs().size()), MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui-title", "<gold>Song Selection")));
        populateCachedGUI();
    }

    private void populateCachedGUI() {
        populateInventory(cachedGUI);
    }

    @Override
    public Inventory getInventory() {
        return getClonedGUIForPlayer();
    }

    public Inventory getClonedGUIForPlayer() {
        Component title = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui-title", "<gold>Song Selection"));
        Inventory playerGUI = Bukkit.createInventory(this, cachedGUI.getSize(), title);
        playerGUI.setContents(cachedGUI.getContents().clone());
        return playerGUI;
    }

    private ParseConfigValues parseSongDetails(String displayName, String artist) {
        Component displayNameComponent = miniMessage.deserialize(displayName);
        Component artistComponent = miniMessage.deserialize(artist);

        String parsedDisplayName = legacySerializer.serialize(displayNameComponent);
        String parsedArtist = legacySerializer.serialize(artistComponent);

        return new ParseConfigValues(parsedDisplayName, parsedArtist);
    }


    private void populateInventory(Inventory cachedGUI) {
        cachedGUI.clear();
        List<Song> songs = songManager.getSongs();

        // Extract material and custom model data from config
        String songMaterialConfig = plugin.getConfig().getString("song-material", "NOTE_BLOCK");
        String[] splitData = songMaterialConfig.split("#");

        Material songItemMaterial;
        try {
            songItemMaterial = Material.valueOf(splitData[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            songItemMaterial = Material.NOTE_BLOCK;
            plugin.getLogger().warning("Invalid song-material in config.yml. Defaulting to NOTE_BLOCK.");
        }

        int customModelData = -1;
        if (splitData.length > 1) {
            try {
                customModelData = Integer.parseInt(splitData[1]);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid custom model data in config.yml for song-material.");
            }
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, songs.size());

        for (int i = startIndex; i < endIndex; i++) {
            Song song = songs.get(i);
            ItemStack item = new ItemStack(songItemMaterial);
            ItemMeta songMeta = item.getItemMeta();

            // Set metadata for song using the actual song name
            PersistentDataContainer container = songMeta.getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "songName"), PersistentDataType.STRING, song.getName());

            // Set the song display name for player visibility
            ParseConfigValues parsedDetails = parseSongDetails(song.getDisplayName(), song.getArtist());
            songMeta.setDisplayName("§6" + parsedDetails.displayName());

            // Add artist credit to the lore
            List<String> lore = new ArrayList<>();
            lore.add("§7By " + parsedDetails.artist());
            songMeta.setLore(lore);

            // Add custom model data to song items
            if (customModelData != -1) {
                songMeta.setCustomModelData(customModelData);
            }

            item.setItemMeta(songMeta);
            cachedGUI.setItem(i - startIndex, item);
        }

        int invSize = cachedGUI.getSize();

        // Previous page
        if (currentPage > 0) {
            addNavigationItem(Material.ARROW, "§aPrevious Page", "previousPage", invSize - 9); //First slot of bottom row
        }

        // Next page
        if (endIndex < songs.size()) {
            addNavigationItem(Material.ARROW, "§aNext Page", "nextPage", invSize - 1); //Last slot of bottom row
        }

        // Stop Song
        addNavigationItem(Material.BARRIER, "§cStop Song", "stopSong", invSize - 5); //Middle slot of bottom row

        // Vote skip button
        addNavigationItem(Material.ARROW, "§6Vote to Skip", "voteSkip", invSize - 7); // 3rd slot from the left

        // Now playing info
        Song currentSong = songManager.getCurrentSong();
        if (currentSong != null) {
            addNowPlayingInfo(currentSong, invSize - 8, cachedGUI); // 2nd slot from the left
        }
    }

    private void addNavigationItem(Material material, String displayName, String action, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);

        // Set metadata for navigation
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, "action"), PersistentDataType.STRING, action);

        item.setItemMeta(meta);
        cachedGUI.setItem(slot, item);
    }

    private void addNowPlayingInfo(Song song, int slot, Inventory cachedGUI) {
        if (song == null) {
            return;
        }

        UUID addedByUUID = song.getAddedByUUID();
        String playerName = "Unknown Player";
        if (addedByUUID != null) {
            playerName = Bukkit.getOfflinePlayer(addedByUUID).getName();
        }

        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Currently Playing");
        ParseConfigValues parsedDetails = parseSongDetails(song.getDisplayName(), song.getArtist());

        List<String> lore = new ArrayList<>();
        lore.add("§eTitle: §7" + parsedDetails.displayName());
        lore.add("§eArtist: §7" + parsedDetails.artist());
        lore.add("§eAdded by: §7" + playerName);
        meta.setLore(lore);

        item.setItemMeta(meta);
        cachedGUI.setItem(slot, item);
    }

    public void updateNowPlayingInfo() {
        Song currentSong = songManager.getCurrentSong();
        if (currentSong != null) {
            addNowPlayingInfo(currentSong, cachedGUI.getSize() - 8, cachedGUI);
        } else {
            cachedGUI.setItem(cachedGUI.getSize() - 8, new ItemStack(Material.AIR));
        }
    }

    public void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < songManager.getSongs().size()) {
            currentPage++;
            populateInventory(cachedGUI);
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            populateInventory(cachedGUI);
        }
    }

    private int getInventorySize(int numSongs) {
        int rowsForSongs = (int) Math.ceil(numSongs / 9.0);
        return (rowsForSongs + 1) * 9; // +1 is for the navigation bar that lets you control the music
    }

    public NPC getBardNpc() {
        return bardNpc;
    }
}
