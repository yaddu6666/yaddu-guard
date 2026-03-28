package fun.yaddu.guard.cache;

import fun.yaddu.guard.api.ModerationResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.UUID;
import java.util.concurrent.*;

public class ModerationCache {

    // Cache: normalized message -> result (max 500 entries, 10 min TTL)
    private static final int MAX_SIZE = 500;
    private static final long TTL_MS = 10 * 60 * 1000L;

    private static final LinkedHashMap<String, CacheEntry> cache = new LinkedHashMap<>(MAX_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > MAX_SIZE;
        }
    };

    // Per-player cooldown: UUID -> last check time
    private static final ConcurrentHashMap<UUID, Long> playerCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 3000L; // 3 seconds per player

    public static synchronized ModerationResult get(String message) {
        String key = normalize(message);
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
            cache.remove(key);
            return null;
        }
        return entry.result;
    }

    public static synchronized void put(String message, ModerationResult result) {
        cache.put(normalize(message), new CacheEntry(result));
    }

    public static boolean isOnCooldown(UUID uuid) {
        Long last = playerCooldown.get(uuid);
        if (last == null) return false;
        return System.currentTimeMillis() - last < COOLDOWN_MS;
    }

    public static void setCooldown(UUID uuid) {
        playerCooldown.put(uuid, System.currentTimeMillis());
    }

    public static void removePlayer(UUID uuid) {
        playerCooldown.remove(uuid);
    }

    private static String normalize(String msg) {
        return msg.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static class CacheEntry {
        final ModerationResult result;
        final long timestamp;
        CacheEntry(ModerationResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
