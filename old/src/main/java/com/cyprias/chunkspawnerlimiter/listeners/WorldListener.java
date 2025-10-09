package com.cyprias.chunkspawnerlimiter.listeners;


import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityChunkInspectorScheduler;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.jetbrains.annotations.NotNull;

public class WorldListener implements Listener {
    private final ChunkSpawnerLimiter plugin;
    private final EntityChunkInspectorScheduler chunkInspectorScheduler;

    public WorldListener(ChunkSpawnerLimiter plugin, EntityChunkInspectorScheduler chunkInspectorScheduler) {
        this.plugin = plugin;
        this.chunkInspectorScheduler = chunkInspectorScheduler;
    }

    @EventHandler
    public void onChunkLoadEvent(@NotNull ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();

        if (plugin.getCslConfig().isActiveInspections()) {
            chunkInspectorScheduler.scheduleInspection(chunk, true);
        }

        if (plugin.getCslConfig().isCheckChunkLoad()) {
            ChatUtil.debug(Debug.CHUNK_LOAD_EVENT, chunk.getX(), chunk.getZ());
            chunkInspectorScheduler.scheduleInspection(chunk, false);
        }
    }

    @EventHandler
    public void onChunkUnloadEvent(@NotNull ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        chunkInspectorScheduler.cancelExistingTask(chunk);

        if (plugin.getCslConfig().isCheckChunkUnload()) {
            ChatUtil.debug(Debug.CHUNK_UNLOAD_EVENT, chunk.getX(), chunk.getZ());
            chunkInspectorScheduler.scheduleInspection(chunk, false);
        }
    }
}
