package com.cyprias.chunkspawnerlimiter.listeners;

import java.util.*;

import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityChunkInspector;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;
import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityInspectTask;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class WorldListener implements Listener {
    private final EntityChunkInspector entityChunkInspector;
    private final ChunkSpawnerLimiter plugin;
    private final Map<Chunk, Integer> chunkTasks = new WeakHashMap<>(); //suspect mem leak

    public WorldListener(final ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
        this.entityChunkInspector = plugin.getChunkInspector();
    }

    @EventHandler
    public void onChunkLoadEvent(@NotNull ChunkLoadEvent event) {
        if (plugin.getCslConfig().isActiveInspections()) {
            //ChatUtil.debug(Debug.CREATE_ACTIVE_CHECK,event.getChunk().getX(), event.getChunk().getZ());
            EntityInspectTask entityInspectTask = new EntityInspectTask(event.getChunk(), entityChunkInspector);
            long delay = plugin.getCslConfig().getInspectionFrequency() * 20L;
            BukkitTask task = entityInspectTask.runTaskTimer(plugin, delay, delay);
            entityInspectTask.setId(task.getTaskId());
            chunkTasks.put(event.getChunk(), task.getTaskId());
        }

        if (plugin.getCslConfig().isCheckChunkLoad()) {
            ChatUtil.debug(Debug.CHUNK_LOAD_EVENT, event.getChunk().getX(), event.getChunk().getZ());
            entityChunkInspector.checkChunk(event.getChunk());
        }
    }

    @EventHandler
    public void onChunkUnloadEvent(@NotNull ChunkUnloadEvent event) {
        if (chunkTasks.containsKey(event.getChunk())) {
            plugin.getServer().getScheduler().cancelTask(chunkTasks.get(event.getChunk()));
            chunkTasks.remove(event.getChunk());
        }

        if (plugin.getCslConfig().isCheckChunkUnload()) {
            ChatUtil.debug(Debug.CHUNK_UNLOAD_EVENT, event.getChunk().getX(), event.getChunk().getZ());
            entityChunkInspector.checkChunk(event.getChunk());
        }
    }
}
