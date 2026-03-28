package fun.yaddu.guard.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class GuardConfig {

    private final Path dataDirectory;
    private final Logger logger;
    private Map<String, Object> data;

    public GuardConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void load() {
        try {
            Files.createDirectories(dataDirectory);
            Path configFile = dataDirectory.resolve("config.yml");

            // Copy default config if not exists
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                        logger.info("Default config.yml created.");
                    }
                }
            }

            // Load config
            Yaml yaml = new Yaml();
            try (InputStream in = Files.newInputStream(configFile)) {
                data = yaml.load(in);
            }
            logger.info("Config loaded successfully!");

        } catch (IOException e) {
            logger.error("Failed to load config: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T def) {
        try {
            String[] parts = path.split("\\.");
            Object current = data;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else return def;
            }
            return current == null ? def : (T) current;
        } catch (Exception e) {
            return def;
        }
    }

    public String getProvider() {
        return get("ai.provider", "gemini");
    }

    public String getGeminiApiKey() {
        return get("ai.gemini.api-key", "");
    }

    public String getGeminiModel() {
        return get("ai.gemini.model", "gemini-1.5-flash");
    }

    public String getGrokApiKey() {
        return get("ai.grok.api-key", "");
    }

    public String getGrokModel() {
        return get("ai.grok.model", "grok-beta");
    }

    public String getAiContext() {
        return get("ai.context", "Analyze this Minecraft chat message for toxicity. Return JSON: {\"toxic\":bool,\"severity\":0-10,\"reason\":\"string\",\"category\":\"string\"}");
    }

    public int getSoftMuteThreshold() { return get("thresholds.soft-mute", 3); }
    public int getKickThreshold() { return get("thresholds.kick", 5); }
    public int getBanThreshold() { return get("thresholds.ban", 7); }
    public int getIpBanThreshold() { return get("thresholds.ip-ban", 9); }

    public int getSoftMuteDuration() { return get("punishments.soft-mute.duration-seconds", 300); }
    public String getSoftMuteMessage() { return get("punishments.soft-mute.message", "&cYou have been soft muted!"); }
    public String getKickMessage() { return get("punishments.kick.message", "&cKicked for toxic behavior: {reason}"); }
    public String getBanCommand() { return get("punishments.ban.command", "ban {player} 1h {reason}"); }
    public String getIpBanCommand() { return get("punishments.ip-ban.command", "banip {player} {reason}"); }
    public boolean isJailEnabled() { return get("punishments.jail.enabled", false); }
    public String getJailCommand() { return get("punishments.jail.command", "jail {player} toxicjail"); }

    public int getEscalateAfter() { return get("violations.escalate-after", 3); }
    public int getViolationResetMinutes() { return get("violations.reset-after-minutes", 30); }

    public String getBlockedMessage() { return get("messages.blocked", "&cMessage blocked by AI moderation."); }
    public String getMutedMessage() { return get("messages.muted", "&cYou are currently soft muted!"); }
    public String getWarningMessage() { return get("messages.warning", "&eWarning: {reason}"); }
    public String getAdminNotifyMessage() { return get("messages.admin-notify", "&8[&cYadduGuard&8] &e{player} &7flagged: &f{reason}"); }

    public String getBypassPermission() { return get("permissions.bypass", "yaddguard.bypass"); }
    public String getAdminPermission() { return get("permissions.admin", "yaddguard.admin"); }

    public boolean isDebug() { return get("debug", false); }

    public String getOpenAiApiKey() { return get("ai.openai.api-key", ""); }
    public String getOpenAiModel() { return get("ai.openai.model", "gpt-4o-mini"); }


    public Map<String, Object> getRawData() { return data; }
}