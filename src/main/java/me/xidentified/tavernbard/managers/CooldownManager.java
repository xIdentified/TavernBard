package me.xidentified.tavernbard.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    public static final long ADD_SONG_COOLDOWN = TimeUnit.SECONDS.toMillis(60);  // 60 seconds cooldown

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + ADD_SONG_COOLDOWN);
    }

    public long getTimeLeft(Player player) {
        return cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
    }

    public boolean isOnCooldown(Player player) {
        return cooldowns.containsKey(player.getUniqueId()) && getTimeLeft(player) > 0;
    }

}
