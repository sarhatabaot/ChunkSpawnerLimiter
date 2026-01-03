package com.github.sarhatabaot.chunkspawnerlimiter;


import com.github.sarhatabaot.chunkspawnerlimiter.command.AdminCommand;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.ChunkListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.EventListener;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.ExternalChecks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
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

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        CSLLogger.setup(this.pluginConfig);
        Checks.setup(pluginConfig);
        ExternalChecks.setup(this.pluginConfig);

        this.counterDataManager = new CounterDataManager();
        this.removalTaskManager = new RemovalTaskManager(this, counterDataManager, pluginConfig);
        this.notificationService = new NotificationService(pluginConfig);

        try {
            CommandFramework commandFramework = new CommandFramework(this);
            commandFramework.registerCommands(new AdminCommand(this, removalTaskManager, pluginConfig));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Command Framework has not been relocated")) {
                // During testing, the library may not be relocated, skip command framework initialization
                getLogger().fine("Command Framework initialization skipped during testing: " + e.getMessage());
            } else {
                throw e;
            }
        }

        RemovalMode.setup(removalTaskManager);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ChunkListener(this, pluginConfig, counterDataManager, removalTaskManager), this);
        pluginManager.registerEvents(new EventListener(pluginConfig, counterDataManager, notificationService), this);

        if (pluginConfig.isMetrics()) {
            try {
                Metrics metrics = new Metrics(this, 4195);
                metrics.addCustomChart(new SimplePie("removal_mode", () -> pluginConfig.getRemovalMode().getKey()));
                //entities removed
                //blocks removed
                //average settings?
            } catch (Exception e) {
                // Silently skip bStats initialization if classes are not available (e.g., during testing)
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

    public ChunkSpawnerLimiter() {
        super();
    }

    protected ChunkSpawnerLimiter(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }
}
