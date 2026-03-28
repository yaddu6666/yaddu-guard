package fun.yaddu.guard;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fun.yaddu.guard.command.GuardCommand;
import fun.yaddu.guard.config.GuardConfig;
import fun.yaddu.guard.listener.ChatListener;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "yaddguard",
    name = "YadduGuard",
    version = "1.0.0",
    description = "AI-powered chat moderation for YadduNetwork",
    authors = {"YadduNetwork"}
)
public class YadduGuard {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private GuardConfig config;
    private static YadduGuard instance;

    @Inject
    public YadduGuard(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("YadduGuard booting up... AI Moderation Loading!");

        // Load config
        this.config = new GuardConfig(dataDirectory, logger);
        config.load();

        // Register chat listener
        server.getEventManager().register(this, new ChatListener(this));

        // Register command
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("yaddguard")
                .aliases("yg", "guard")
                .build(),
            new GuardCommand(this)
        );

        logger.info("YadduGuard loaded! Provider: " + config.getProvider().toUpperCase());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("YadduGuard shutting down. Bhul mat bhai, toxic mat ho!");
    }

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public GuardConfig getGuardConfig() { return config; }
    public static YadduGuard getInstance() { return instance; }
    public Path getDataDirectory() { return dataDirectory; }
}
