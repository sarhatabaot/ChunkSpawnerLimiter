package com.cyprias.chunkspawnerlimiter.inspection.entities;


import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityChunkInspectorScheduler {
    private final ChunkSpawnerLimiter plugin;
    private final EntityChunkInspector entityChunkInspector;
    private final Map<Chunk, BukkitTask> chunkTasks = new WeakHashMap<>();

    public EntityChunkInspectorScheduler(ChunkSpawnerLimiter plugin, EntityChunkInspector entityChunkInspector) {
        this.plugin = plugin;
        this.entityChunkInspector = entityChunkInspector;
    }

    /**
     * Schedules an inspection task for the chunk
     * @param chunk The chunk to inspect
     * @param repeating Whether the task should repeat
     */
    public void scheduleInspection(@NotNull Chunk chunk, boolean repeating) {
        cancelExistingTask(chunk);

        EntityInspectTask task = new EntityInspectTask(chunk, entityChunkInspector);
        long delay = plugin.getCslConfig().getInspectionFrequency() * 20L;

        BukkitTask bukkitTask = repeating ?
                task.runTaskTimer(plugin, delay, delay) :
                task.runTaskLater(plugin, delay);

        task.setId(bukkitTask.getTaskId());
        chunkTasks.put(chunk, bukkitTask);
    }

    /**
     * Cancels any existing task for the chunk, also remove from map.
     * @param chunk The chunk to cancel tasks for
     */
    public void cancelExistingTask(@NotNull Chunk chunk) {
        BukkitTask task = chunkTasks.remove(chunk);
        if (task != null) {
            task.cancel();
        }
    }
}
