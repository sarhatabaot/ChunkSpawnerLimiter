package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class RemovalTaskManager {
    private final Queue<QueuedCheck> pendingChunks = new ConcurrentLinkedQueue<>();
    private final Set<ChunkCoord> queuedChunks =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final CounterDataManager counterDataManager;
    private final ChunkSpawnerLimiter plugin;
    private final PluginConfig pluginConfig;

    public RemovalTaskManager(ChunkSpawnerLimiter plugin, CounterDataManager counterDataManager, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.pluginConfig = pluginConfig;
        startProcessingTask();
    }

    public void queueChunkCheck(ChunkCoord coord, Consumer<Entity> action) {
        if (queuedChunks.add(coord)) {
            pendingChunks.add(new QueuedCheck(coord, action));
        }
    }


    private void startProcessingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 20L, 20L); // every 1 second
    }

    private void processQueue() {
        QueuedCheck check;
        while ((check = pendingChunks.poll()) != null) {
            processChunk(check.coord, check.action);
            queuedChunks.remove(check.coord);
        }
    }

    public void processChunk(ChunkCoord coord, Consumer<Entity> removalAction) {
        CounterData data = counterDataManager.getCounterData(coord);
        if (data == null) return;

        Chunk chunk = coord.getChunk();
        if (chunk == null || !chunk.isLoaded()) {
            return;
        }
        for (EntityType type : data.getTrackedEntityTypes()) {
            List<Entity> entities = Arrays.stream(chunk.getEntities())
                    .filter(e -> e.getType() == type)
                    .toList();

            Integer allowed = pluginConfig.getEntityLimit(type);
            if (allowed == null) {
                CSLLogger.debug("No limit found for entity type: %s, skipping".formatted(type.name()));
                continue;
            }

            if (entities.size() > allowed) {
                int toRemove = entities.size() - allowed;
                for (int i = 0; i < toRemove; i++) {
                    removalAction.accept(entities.get(i));
                }
            }
        }
    }

    //todo make this not static.
    public static boolean isUnderOrEqualToLimit(int count, int limit) {
        return count + 1 <= limit;
    }

    private static class QueuedCheck {
        final ChunkCoord coord;
        final Consumer<Entity> action;

        QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
            this.coord = coord;
            this.action = action;
        }
    }
}
