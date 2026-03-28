package fun.yaddu.guard.punishment;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fun.yaddu.guard.YadduGuard;
import fun.yaddu.guard.api.ModerationResult;
import fun.yaddu.guard.config.GuardConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;


public class PunishmentManager {

    private final YadduGuard plugin;
    private final GuardConfig config;
    private final ProxyServer server;

    public PunishmentManager(YadduGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getGuardConfig();
        this.server = plugin.getServer();
    }

    /**
     * Apply punishment based on AI moderation result and violation history.
     * Returns true if message should be blocked.
     */
    public boolean handleViolation(Player player, ModerationResult result) {
        int severity = result.getSeverity();
        String reason = result.getReason();

        // Add to violation tracker
        int violationCount = ViolationTracker.addViolation(player.getUniqueId(), severity);

        // Notify admins
        notifyAdmins(player, result, violationCount);

        // Determine punishment based on severity
        if (severity >= config.getIpBanThreshold()) {
            executeIpBan(player, reason);
            return true;
        } else if (severity >= config.getBanThreshold()) {
            // Check if jail is enabled and this is their first severe offense
            if (config.isJailEnabled() && violationCount <= 2) {
                executeJail(player, reason);
            } else {
                executeBan(player, reason);
            }
            return true;
        } else if (severity >= config.getKickThreshold()) {
            // If repeated violations, escalate to ban
            if (violationCount >= config.getEscalateAfter()) {
                executeBan(player, reason + " (repeated violations)");
            } else {
                executeKick(player, reason);
            }
            return true;
        } else if (severity >= config.getSoftMuteThreshold()) {
            // Soft mute - block this message and mute
            if (!ViolationTracker.isSoftMuted(player.getUniqueId())) {
                executeSoftMute(player);
            } else {
                // Already muted - if repeated, kick
                if (violationCount >= config.getEscalateAfter()) {
                    executeKick(player, "Continued toxic behavior while muted");
                }
            }
            return true; // Block the message
        }

        return false; // Don't block
    }

    public void executeSoftMute(Player player) {
        int duration = config.getSoftMuteDuration();
        ViolationTracker.softMute(player.getUniqueId(), duration);

        player.sendMessage(colorize(config.getSoftMuteMessage()));
        plugin.getLogger().info("[YadduGuard] Soft muted: " + player.getUsername() + " for " + duration + "s");
    }

    public void executeKick(Player player, String reason) {
        String msg = config.getKickMessage().replace("{reason}", reason);
        player.disconnect(colorize(msg));
        plugin.getLogger().info("[YadduGuard] Kicked: " + player.getUsername() + " | Reason: " + reason);
    }

    public void executeBan(Player player, String reason) {
        String cmd = config.getBanCommand()
            .replace("{player}", player.getUsername())
            .replace("{reason}", reason);
        executeConsoleCommand(cmd);
        player.disconnect(colorize("&cYou have been banned for toxic behavior.\n&7Reason: &f" + reason));
        plugin.getLogger().info("[YadduGuard] Banned: " + player.getUsername() + " | Reason: " + reason);
    }

    public void executeIpBan(Player player, String reason) {
        String cmd = config.getIpBanCommand()
            .replace("{player}", player.getUsername())
            .replace("{reason}", reason);
        executeConsoleCommand(cmd);
        player.disconnect(colorize("&cYou have been IP banned for extreme toxicity.\n&7Reason: &f" + reason));
        plugin.getLogger().info("[YadduGuard] IP Banned: " + player.getUsername() + " | Reason: " + reason);
    }

    public void executeJail(Player player, String reason) {
        String cmd = config.getJailCommand()
            .replace("{player}", player.getUsername());
        executeConsoleCommand(cmd);
        player.sendMessage(colorize("&cYou have been jailed for toxic behavior! Reason: &f" + reason));
        plugin.getLogger().info("[YadduGuard] Jailed: " + player.getUsername() + " | Reason: " + reason);
    }

    private void executeConsoleCommand(String command) {
        plugin.getLogger().info("[YadduGuard] Executing: " + command);
        // Velocity doesn't have a direct "console command" executor for backend commands.
        // Commands like /ban need to be sent to the backend server or via a messaging channel.
        // We use the built-in command manager for proxy-level commands:
        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
    }

    private void notifyAdmins(Player player, ModerationResult result, int violations) {
        String msg = config.getAdminNotifyMessage()
            .replace("{player}", player.getUsername())
            .replace("{reason}", result.getReason())
            .replace("{severity}", String.valueOf(result.getSeverity()))
            .replace("{category}", result.getCategory())
            .replace("{violations}", String.valueOf(violations));

        Component adminMsg = colorize(msg);

        server.getAllPlayers().stream()
            .filter(p -> p.hasPermission(config.getAdminPermission()))
            .forEach(p -> p.sendMessage(adminMsg));

        // Also log to console
        plugin.getLogger().warn("[YadduGuard] Flag: " + player.getUsername()
            + " | Severity: " + result.getSeverity()
            + " | " + result.getReason()
            + " | Violations: " + violations);
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
