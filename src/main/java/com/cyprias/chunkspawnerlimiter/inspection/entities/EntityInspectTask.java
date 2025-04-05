package com.cyprias.chunkspawnerlimiter.inspection.entities;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.ref.WeakReference;


/**
 * A BukkitRunnable task that inspects a chunk for potential spawner issues.
 * The task checks if the chunk is loaded and then calls the checkChunk method.
 * If the chunk is not loaded, the task cancels itself.
 */
public class EntityInspectTask extends BukkitRunnable {
    private final EntityChunkInspector entityChunkInspector;
    /**
     * A WeakReference to the chunk being inspected.
     * This is used to prevent memory leaks when the chunk is garbage collected.
     */
    private final WeakReference<Chunk> refChunk;

    /**
     * The ID of the task, used to cancel the task if necessary.
     */
    private int id;

    /**
     * {@inheritDoc}
     *
     * Performs the inspection of the chunk.
     * If the chunk is null, logs a message and returns.
     * If the chunk is not loaded, cancels the task.
     * Otherwise, calls the checkChunk method.
     */
    @Override
    public void run() {
        final Chunk chunk = this.refChunk.get();
        if (chunk == null || !chunk.isLoaded()) {
            Bukkit.getLogger().fine("Chunk is null or unloaded! Ignoring");
            return;
        }

        ChatUtil.debug(Debug.ACTIVE_CHECK, chunk.getX(), chunk.getZ());
        if (!chunk.isLoaded()) {
            ChunkSpawnerLimiter.cancelTask(id);
            return;
        }

        entityChunkInspector.checkChunk(chunk);
    }

    /**
     * Constructs a new InspectTask for the given chunk.
     *
     * @param chunk The chunk to be inspected
     */
    public EntityInspectTask(final Chunk chunk, final EntityChunkInspector entityChunkInspector) {
        this.refChunk = new WeakReference<>(chunk);
        this.entityChunkInspector = entityChunkInspector;
    }

    /**
     * Sets the ID of the task.
     *
     * @param id The ID of the task
     */
    public void setId(final int id) {
        this.id = id;
    }
}
