package com.github.sarhatabaot.chunkspawnerlimiter;


import com.github.sarhatabaot.chunkspawnerlimiter.command.AdminCommand;
import me.despical.commandframework.CommandFramework;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkSpawnerLimiter extends JavaPlugin {
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        CommandFramework commandFramework = new CommandFramework(this);
        commandFramework.registerCommands(new AdminCommand(this));

        if (pluginConfig.isMetrics()) {
            Metrics metrics = new Metrics(this, 4195);
            metrics.addCustomChart(new SimplePie("removal-mode", () -> pluginConfig.getRemovalMode().getKey()));
        }
    }

    @Override
    public void onDisable() {

    }


}
