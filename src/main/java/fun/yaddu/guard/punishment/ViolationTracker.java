package fun.yaddu.guard.punishment;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationTracker {

    private static final Map<UUID, PlayerViolations> violationMap = new ConcurrentHashMap<>();

    public static int addViolation(UUID uuid, int severity) {
        violationMap.compute(uuid, (k, v) -> {
            if (v == null) return new PlayerViolations(severity);
            v.add(severity);
            return v;
        });
        return violationMap.get(uuid).getCount();
    }

    public static int getViolationCount(UUID uuid) {
        PlayerViolations pv = violationMap.get(uuid);
        return pv == null ? 0 : pv.getCount();
    }

    public static int getTotalSeverity(UUID uuid) {
        PlayerViolations pv = violationMap.get(uuid);
        return pv == null ? 0 : pv.getTotalSeverity();
    }

    public static void reset(UUID uuid) {
        violationMap.remove(uuid);
    }

    public static void resetAll() {
        violationMap.clear();
    }

    // Clean stale violations older than N minutes
    public static void cleanStale(int minutes) {
        Instant cutoff = Instant.now().minusSeconds((long) minutes * 60);
        violationMap.entrySet().removeIf(e -> e.getValue().getLastViolationTime().isBefore(cutoff));
    }

    public static class PlayerViolations {
        private int count = 0;
        private int totalSeverity = 0;
        private Instant lastViolationTime = Instant.now();

        public PlayerViolations(int severity) {
            add(severity);
        }

        public void add(int severity) {
            count++;
            totalSeverity += severity;
            lastViolationTime = Instant.now();
        }

        public int getCount() { return count; }
        public int getTotalSeverity() { return totalSeverity; }
        public Instant getLastViolationTime() { return lastViolationTime; }
    }

    // Soft mute tracking
    private static final Map<UUID, Long> softMutedPlayers = new ConcurrentHashMap<>();

    public static void softMute(UUID uuid, int durationSeconds) {
        softMutedPlayers.put(uuid, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public static boolean isSoftMuted(UUID uuid) {
        Long expiry = softMutedPlayers.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            softMutedPlayers.remove(uuid);
            return false;
        }
        return true;
    }

    public static void unmute(UUID uuid) {
        softMutedPlayers.remove(uuid);
    }

    public static long getMuteRemainingSeconds(UUID uuid) {
        Long expiry = softMutedPlayers.get(uuid);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
}
