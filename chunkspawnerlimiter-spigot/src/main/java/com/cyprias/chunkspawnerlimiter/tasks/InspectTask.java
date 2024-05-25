package com.cyprias.chunkspawnerlimiter.tasks;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;

import org.bukkit.Chunk;
import org.bukkit.scheduler.BukkitRunnable;

import static com.cyprias.chunkspawnerlimiter.listeners.WorldListener.checkChunk;

public class InspectTask extends BukkitRunnable {
    private final Chunk chunk;
    private int id;

    @Override
    public void run() {
        ChatUtil.debug(Debug.ACTIVE_CHECK,chunk.getX(),chunk.getZ());
        if (!chunk.isLoaded()) {
            ChunkSpawnerLimiter.cancelTask(id);
            return;
        }
        checkChunk(chunk);
    }

    public InspectTask(final Chunk chunk) {
        this.chunk = chunk;
    }

    public void setId(final int id) {
        this.id = id;
    }
}
