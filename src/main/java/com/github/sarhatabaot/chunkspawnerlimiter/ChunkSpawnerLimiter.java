package com.github.sarhatabaot.chunkspawnerlimiter;


import com.github.sarhatabaot.chunkspawnerlimiter.command.AdminCommand;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.ChunkListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.EventListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.DespawnListener;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.ExternalChecks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import me.despical.commandframework.CommandFramework;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;

public class ChunkSpawnerLimiter extends JavaPlugin {
    private RemovalTaskManager removalTaskManager;
    private CounterDataManager counterDataManager;
    private PluginConfig pluginConfig;
    private NotificationService notificationService;
    private EntityChunkTracker entityChunkTracker;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        CSLLogger.setup(this.pluginConfig);
        Checks.setup(pluginConfig);
        ExternalChecks.setup(this.pluginConfig);

        this.counterDataManager = new CounterDataManager();
        this.removalTaskManager = new RemovalTaskManager(this, counterDataManager, pluginConfig);
        this.notificationService = new NotificationService(pluginConfig);

        // Entity chunk tracker for cross-chunk movement detection (polls every 2s / 40 ticks)
        this.entityChunkTracker = new EntityChunkTracker(
                this,
                counterDataManager,
                pluginConfig::hasResolvedEntityLimit,
                40L
        );

        try {
            CommandFramework commandFramework = new CommandFramework(this);
            commandFramework.registerCommands(new AdminCommand(this, removalTaskManager, pluginConfig));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Command Framework has not been relocated")) {
                getLogger().fine("Command Framework initialization skipped during testing: " + e.getMessage());
            } else {
                throw e;
            }
        }

        RemovalMode.setup(removalTaskManager);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ChunkListener(this, pluginConfig, counterDataManager, removalTaskManager, entityChunkTracker), this);
        pluginManager.registerEvents(new EventListener(this, pluginConfig, counterDataManager, notificationService, entityChunkTracker), this);
        DespawnListener.registerIfSupported(this, pluginConfig, counterDataManager, entityChunkTracker);

        if (pluginConfig.isMetrics()) {
            try {
                Metrics metrics = new Metrics(this, 4195);
                metrics.addCustomChart(new SimplePie("removal_mode", () -> pluginConfig.getRemovalMode().getKey()));
            } catch (Exception e) {
                getLogger().fine("bStats metrics initialization skipped: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        this.counterDataManager = null;
        this.removalTaskManager = null;
        this.pluginConfig = null;
        this.notificationService = null;
        this.entityChunkTracker = null;
    }

    public void onReload() {
        this.pluginConfig.reload();
    }

    public CounterDataManager getCounterDataManager() {
        return counterDataManager;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public RemovalTaskManager getRemovalTaskManager() {
        return removalTaskManager;
    }

    public EntityChunkTracker getEntityChunkTracker() {
        return entityChunkTracker;
    }

    public ChunkSpawnerLimiter() {
        super();
    }

    protected ChunkSpawnerLimiter(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }
}