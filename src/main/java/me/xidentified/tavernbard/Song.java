package me.xidentified.tavernbard;

import org.bukkit.Bukkit;

import java.util.UUID;

public class Song {
    protected final String namespace;
    protected final String name;
    protected final String displayName;
    protected final String artist;
    protected final int duration;
    protected final UUID addedByUUID;

    public Song(String namespace, String name, String displayName, String artist, int duration, UUID addedByUUID) {
        this.namespace = namespace;
        this.name = name;
        this.displayName = displayName;
        this.artist = artist;
        this.duration = duration;
        this.addedByUUID = addedByUUID;
    }

    public Song(String namespace, String name, String displayName, String artist, int duration) {
        this(namespace, name, displayName, artist, duration, null);
    }

    public String getAddedByName() {
        if (addedByUUID == null) return "Unknown Player";
        return Bukkit.getOfflinePlayer(addedByUUID).getName();
    }

    public UUID getAddedByUUID() {
        return addedByUUID;
    }

    public String getSoundReference() {
        return namespace + ":" + name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
