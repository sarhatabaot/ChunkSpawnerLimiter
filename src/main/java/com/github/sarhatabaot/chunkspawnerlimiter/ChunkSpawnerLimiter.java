package com.github.sarhatabaot.chunkspawnerlimiter;


import com.github.sarhatabaot.chunkspawnerlimiter.command.AdminCommand;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.ChunkListener;
import com.github.sarhatabaot.chunkspawnerlimiter.listener.EventListener;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.ExternalChecks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import me.despical.commandframework.CommandFramework;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkSpawnerLimiter extends JavaPlugin {
    private RemovalTaskManager removalTaskManager;
    private CounterDataManager counterDataManager;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        CSLLogger.setup(this.pluginConfig);
        Checks.setup(pluginConfig);
        ExternalChecks.setup(this.pluginConfig);

        CommandFramework commandFramework = new CommandFramework(this);
        commandFramework.registerCommands(new AdminCommand(this));

        this.counterDataManager = new CounterDataManager();
        this.removalTaskManager = new RemovalTaskManager(this, counterDataManager, pluginConfig);

        RemovalMode.setup(removalTaskManager);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ChunkListener(pluginConfig, counterDataManager, removalTaskManager), this);
        pluginManager.registerEvents(new EventListener(pluginConfig, counterDataManager), this);

        if (pluginConfig.isMetrics()) {
            Metrics metrics = new Metrics(this, 4195);
            metrics.addCustomChart(new SimplePie("removal_mode", () -> pluginConfig.getRemovalMode().getKey()));
        }
    }

    @Override
    public void onDisable() {
        this.counterDataManager = null;
        this.removalTaskManager = null;
        this.pluginConfig = null;
    }

    public void onReload() {
        this.pluginConfig.reload();
    }


}
