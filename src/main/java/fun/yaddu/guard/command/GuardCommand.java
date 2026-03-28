package fun.yaddu.guard.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fun.yaddu.guard.YadduGuard;
import fun.yaddu.guard.config.GuardConfig;
import fun.yaddu.guard.punishment.PunishmentManager;
import fun.yaddu.guard.punishment.ViolationTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GuardCommand implements SimpleCommand {

    private final YadduGuard plugin;
    private final GuardConfig config;

    public GuardCommand(YadduGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getGuardConfig();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(config.getAdminPermission())) {
            send(source, "&cNo permission bhai!");
            return;
        }

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {

            case "reload" -> {
                config.load();
                send(source, "&aYadduGuard config reloaded!");
            }

            case "status" -> {
                send(source, "&8[&cYadduGuard&8] &aStatus:");
                send(source, "&7Provider: &e" + config.getProvider().toUpperCase());
                send(source, "&7Gemini Key: " + (config.getGeminiApiKey().equals("YOUR_GEMINI_API_KEY") ? "&cNot set" : "&aSet"));
                send(source, "&7Grok Key: " + (config.getGrokApiKey().equals("YOUR_GROK_API_KEY") ? "&cNot set" : "&aSet"));
                send(source, "&7Thresholds: Mute=" + config.getSoftMuteThreshold()
                    + " Kick=" + config.getKickThreshold()
                    + " Ban=" + config.getBanThreshold()
                    + " IPBan=" + config.getIpBanThreshold());
                send(source, "&7Debug: " + (config.isDebug() ? "&aON" : "&cOFF"));
            }

            case "unmute" -> {
                if (args.length < 2) { send(source, "&cUsage: /yg unmute <player>"); return; }
                String targetName = args[1];
                Optional<Player> target = plugin.getServer().getPlayer(targetName);
                if (target.isPresent()) {
                    ViolationTracker.unmute(target.get().getUniqueId());
                    send(source, "&aUnmuted &e" + targetName);
                } else {
                    send(source, "&cPlayer not found: " + targetName);
                }
            }

            case "violations" -> {
                if (args.length < 2) { send(source, "&cUsage: /yg violations <player>"); return; }
                String targetName = args[1];
                Optional<Player> target = plugin.getServer().getPlayer(targetName);
                if (target.isPresent()) {
                    UUID uuid = target.get().getUniqueId();
                    int count = ViolationTracker.getViolationCount(uuid);
                    int total = ViolationTracker.getTotalSeverity(uuid);
                    boolean muted = ViolationTracker.isSoftMuted(uuid);
                    long muteLeft = ViolationTracker.getMuteRemainingSeconds(uuid);
                    send(source, "&7--- &e" + targetName + " &7---");
                    send(source, "&7Violations: &c" + count);
                    send(source, "&7Total Severity: &c" + total);
                    send(source, "&7Soft Muted: " + (muted ? "&cYes (&e" + muteLeft + "s left&c)" : "&aNo"));
                } else {
                    send(source, "&cPlayer not found: " + targetName);
                }
            }

            case "resetviolations", "resetv" -> {
                if (args.length < 2) { send(source, "&cUsage: /yg resetv <player|all>"); return; }
                if (args[1].equalsIgnoreCase("all")) {
                    ViolationTracker.resetAll();
                    send(source, "&aAll violations reset!");
                } else {
                    Optional<Player> target = plugin.getServer().getPlayer(args[1]);
                    if (target.isPresent()) {
                        ViolationTracker.reset(target.get().getUniqueId());
                        send(source, "&aViolations reset for &e" + args[1]);
                    } else {
                        send(source, "&cPlayer not found: " + args[1]);
                    }
                }
            }

            case "test" -> {
                if (args.length < 2) { send(source, "&cUsage: /yg test <message>"); return; }
                String testMsg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                send(source, "&7Testing: &f" + testMsg);
                send(source, "&7Sending to AI...");
                new fun.yaddu.guard.api.AiModerationClient(config, plugin.getLogger())
                    .analyze("TestPlayer", testMsg)
                    .thenAccept(result -> {
                        send(source, "&7Result: Toxic=" + (result.isToxic() ? "&cYes" : "&aNo")
                            + " &7Severity=&e" + result.getSeverity()
                            + " &7Reason=&f" + result.getReason()
                            + " &7Category=&b" + result.getCategory());
                    });
            }

            default -> sendHelp(source);
        }
    }

    private void sendHelp(CommandSource source) {
        send(source, "&8--- &cYadduGuard Commands &8---");
        send(source, "&e/yg status &7- Show plugin status");
        send(source, "&e/yg reload &7- Reload config");
        send(source, "&e/yg unmute <player> &7- Unmute a player");
        send(source, "&e/yg violations <player> &7- Show player violations");
        send(source, "&e/yg resetv <player|all> &7- Reset violations");
        send(source, "&e/yg test <message> &7- Test AI moderation");
    }

    private void send(CommandSource source, String msg) {
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(config.getAdminPermission());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return List.of("status", "reload", "unmute", "violations", "resetv", "test");
        }
        return List.of();
    }
}
