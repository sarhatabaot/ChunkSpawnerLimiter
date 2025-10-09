package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;


public class ChunkListener implements Listener {
    private PluginConfig pluginConfig;

    public ChunkListener(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {

    }
}
