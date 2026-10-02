package com.github.sarhatabaot.chunkspawnerlimiter;


import com.github.sarhatabaot.chunkspawnerlimiter.command.AdminCommand;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.ChunkListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.EventListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.DespawnListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.EntityTransformListener;
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
import org.bukkit.event.HandlerList;
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
    private ChunkListener chunkListener;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        CSLLogger.setup(this.pluginConfig);
        Checks.setup(pluginConfig);
        ExternalChecks.setup(this.pluginConfig);

        registerCommands();

        if (!pluginConfig.isEnabled()) {
            getLogger().info("ChunkSpawnerLimiter logic is disabled in config.yml.");
            return;
        }

        startRuntime(false);

        if (pluginConfig.isMetrics()) {
            try {
                Metrics metrics = new Metrics(this, 4195);
                metrics.addCustomChart(new SimplePie("removal_mode", () -> pluginConfig.getRemovalMode().getKey()));
            } catch (Exception e) {
                getLogger().fine("bStats metrics initialization skipped: " + e.getMessage());
            }
        }
    }

    private void startRuntime(boolean rebuildLoadedChunks) {
        this.counterDataManager = new CounterDataManager();
        this.removalTaskManager = new RemovalTaskManager(this, counterDataManager, pluginConfig);
        this.notificationService = new NotificationService(pluginConfig);

        // Entity chunk tracker for cross-chunk movement detection (polls every 2s / 40 ticks)
        this.entityChunkTracker = new EntityChunkTracker(
                this,
                counterDataManager,
                entity -> Checks.shouldTrackEntity(entity, pluginConfig),
                40L
        );

        RemovalMode.setup(removalTaskManager);

        PluginManager pluginManager = Bukkit.getPluginManager();
        this.chunkListener = new ChunkListener(this, pluginConfig, counterDataManager, removalTaskManager, entityChunkTracker);
        pluginManager.registerEvents(chunkListener, this);
        pluginManager.registerEvents(new EventListener(this, pluginConfig, counterDataManager, notificationService, entityChunkTracker), this);
        DespawnListener.registerIfSupported(this, pluginConfig, counterDataManager, entityChunkTracker);
        EntityTransformListener.registerIfSupported(this, pluginConfig, counterDataManager, entityChunkTracker);

        if (rebuildLoadedChunks) {
            chunkListener.rebuildLoadedChunks();
        }
    }

    private void registerCommands() {
        try {
            CommandFramework commandFramework = new CommandFramework(this);
            commandFramework.registerCommands(new AdminCommand(this, pluginConfig));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Command Framework has not been relocated")) {
                getLogger().fine("Command Framework initialization skipped during testing: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public void onDisable() {
        stopRuntime();
        this.pluginConfig = null;
    }

    private void stopRuntime() {
        if (this.chunkListener != null) {
            this.chunkListener.shutdown();
        }
        try {
            Bukkit.getScheduler().cancelTasks(this);
        } catch (RuntimeException exception) {
            if (!exception.getClass().getName()
                    .equals("be.seeseemelk.mockbukkit.UnimplementedOperationException")) {
                throw exception;
            }
        }
        HandlerList.unregisterAll(this);
        this.counterDataManager = null;
        this.removalTaskManager = null;
        this.notificationService = null;
        this.entityChunkTracker = null;
        this.chunkListener = null;
    }

    public void onReload() {
        stopRuntime();
        this.pluginConfig.reload();
        CSLLogger.setup(this.pluginConfig);
        Checks.setup(pluginConfig);
        ExternalChecks.setup(this.pluginConfig);

        if (!pluginConfig.isEnabled()) {
            getLogger().info("ChunkSpawnerLimiter logic is disabled in config.yml.");
            return;
        }

        startRuntime(true);
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
