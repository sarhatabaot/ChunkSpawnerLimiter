package com.github.sarhatabaot.chunkspawnerlimiter;


import org.bukkit.plugin.java.JavaPlugin;

public class ChunkSpawnerLimiter extends JavaPlugin {
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        // Stats, add for Removal Mode, notifications for now
    }

    @Override
    public void onDisable() {

    }


}
