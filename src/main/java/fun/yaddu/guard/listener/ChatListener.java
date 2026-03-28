package fun.yaddu.guard.listener;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import fun.yaddu.guard.YadduGuard;
import fun.yaddu.guard.api.AiModerationClient;
import fun.yaddu.guard.api.ModerationResult;
import fun.yaddu.guard.cache.ModerationCache;
import fun.yaddu.guard.config.GuardConfig;
import fun.yaddu.guard.filter.QuickFilter;
import fun.yaddu.guard.punishment.PunishmentManager;
import fun.yaddu.guard.punishment.ViolationTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatListener {

    private final YadduGuard plugin;
    private final GuardConfig config;
    private final AiModerationClient aiClient;
    private final PunishmentManager punishmentManager;
    private final ScheduledExecutorService scheduler;

    public ChatListener(YadduGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getGuardConfig();
        this.aiClient = new AiModerationClient(config, plugin.getLogger());
        this.punishmentManager = new PunishmentManager(plugin);
        this.scheduler = Executors.newScheduledThreadPool(2);

        // Clean stale violations every 10 min
        scheduler.scheduleAtFixedRate(
            () -> ViolationTracker.cleanStale(config.getViolationResetMinutes()),
            10, 10, TimeUnit.MINUTES
        );
    }

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.startsWith("/")) return null;
        if (player.hasPermission(config.getBypassPermission())) return null;

        // Soft mute check (instant, no AI)
        if (ViolationTracker.isSoftMuted(player.getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            long rem = ViolationTracker.getMuteRemainingSeconds(player.getUniqueId());
            player.sendMessage(colorize(config.getMutedMessage() + " &7(&e" + rem + "s &7baki)"));
            return null;
        }

        // Quick keyword filter - skip AI for obviously clean messages
        if (!QuickFilter.mightBeToxic(message)) {
            return null; // Allow instantly, no AI call
        }

        // Per-player cooldown - don't spam API
        if (ModerationCache.isOnCooldown(player.getUniqueId())) {
            // Still block if they're soft muted
            return null;
        }

        // Check cache first
        ModerationResult cached = ModerationCache.get(message);
        if (cached != null && !cached.isError()) {
            if (cached.isToxic() || cached.getSeverity() >= config.getSoftMuteThreshold()) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                if (player.isActive()) {
                    punishmentManager.handleViolation(player, cached);
                    if (player.isActive() && cached.getSeverity() < config.getKickThreshold()) {
                        player.sendMessage(colorize(config.getBlockedMessage()));
                        player.sendMessage(colorize(config.getWarningMessage().replace("{reason}", cached.getReason())));
                    }
                }
            }
            return null;
        }

        // Set cooldown before async call
        ModerationCache.setCooldown(player.getUniqueId());

        // Async AI check
        return EventTask.withContinuation(continuation -> {
            aiClient.analyze(player.getUsername(), message).whenComplete((result, ex) -> {
                try {
                    if (ex != null || result == null || result.isError()) {
                        // Fail open on API error
                        event.setResult(PlayerChatEvent.ChatResult.allowed());
                        return;
                    }

                    if (config.isDebug())
                        plugin.getLogger().info("[YadduGuard] " + player.getUsername() + ": " + result);

                    // Cache result
                    ModerationCache.put(message, result);

                    if (result.isToxic() || result.getSeverity() >= config.getSoftMuteThreshold()) {
                        event.setResult(PlayerChatEvent.ChatResult.denied());
                        if (player.isActive()) {
                            punishmentManager.handleViolation(player, result);
                            if (player.isActive() && result.getSeverity() < config.getKickThreshold()) {
                                player.sendMessage(colorize(config.getBlockedMessage()));
                                player.sendMessage(colorize(config.getWarningMessage().replace("{reason}", result.getReason())));
                            }
                        }
                    } else {
                        event.setResult(PlayerChatEvent.ChatResult.allowed());
                    }
                } finally {
                    continuation.resume();
                }
            });
        });
    }

    private Component colorize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
