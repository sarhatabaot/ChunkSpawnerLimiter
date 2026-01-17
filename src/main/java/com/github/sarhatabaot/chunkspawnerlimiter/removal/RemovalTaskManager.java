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
    private final static long TICKS_PER_SECOND = 20L;
    private final Queue<QueuedCheck> pendingChunks = new ConcurrentLinkedQueue<>();
    private final Set<ChunkCoord> queuedChunks =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Map of chunks that should be rechecked after a delay (timestamp in ms)
    private final Map<ChunkCoord, List<DelayedQueuedCheck>> scheduledRechecks = new ConcurrentHashMap<>();

    private final CounterDataManager counterDataManager;
    private final ChunkSpawnerLimiter plugin;
    private final PluginConfig pluginConfig;

    public RemovalTaskManager(ChunkSpawnerLimiter plugin, CounterDataManager counterDataManager, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.pluginConfig = pluginConfig;
        startProcessingTask();
    }

    /**
     * Schedule this chunk to be checked again after X seconds.
     */
    public void scheduleRecheck(ChunkCoord coord, Consumer<Entity> action, long delaySeconds) {
        long nextCheck = System.currentTimeMillis() + (delaySeconds * 1000L);
        scheduledRechecks.computeIfAbsent(coord, k -> new ArrayList<>()).add(new DelayedQueuedCheck(action, nextCheck));
        CSLLogger.debug(() -> "Scheduled recheck for chunk %s in %d seconds".formatted(coord, delaySeconds));
    }


    public void queueChunkCheck(ChunkCoord coord, Consumer<Entity> action) {
        if (queuedChunks.add(coord)) {
            pendingChunks.add(new QueuedCheck(coord, action));
        }
    }

    public void removeChunkRecheck(ChunkCoord coord) {
        scheduledRechecks.remove(coord);
    }

    private void startProcessingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, TICKS_PER_SECOND, TICKS_PER_SECOND); // every 1 second
    }

    private void processQueue() {
        QueuedCheck check;
        while ((check = pendingChunks.poll()) != null) {
            processChunk(check.coord, check.action);
            queuedChunks.remove(check.coord);
        }

        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<ChunkCoord, List<DelayedQueuedCheck>>> it = scheduledRechecks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ChunkCoord, List<DelayedQueuedCheck>> entry = it.next();
            List<DelayedQueuedCheck> list = entry.getValue();
            list.removeIf(delayed -> {
                if (delayed.timestamp <= now) {
                    queueChunkCheck(entry.getKey(), delayed.action);
                    return true;
                }
                return false;
            });
            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    public void processChunk(ChunkCoord coord, Consumer<Entity> removalAction) {
        CounterData data = counterDataManager.getCounterData(coord);
        if (data == null) return;

        Chunk chunk = coord.getChunk();
        if (chunk == null || !chunk.isLoaded()) return;

        // Group entities by tracked type in a single pass
        Map<EntityType, List<Entity>> entitiesByType = new EnumMap<>(EntityType.class);

        for (Entity entity : chunk.getEntities()) {
            EntityType type = entity.getType();

            if (!data.getTrackedEntityTypes().contains(type)) continue;

            entitiesByType
                    .computeIfAbsent(type, t -> new ArrayList<>())
                    .add(entity);
        }

        // Apply resolved limits per entity type (includes group limits already)
        for (Map.Entry<EntityType, List<Entity>> entry : entitiesByType.entrySet()) {
            EntityType type = entry.getKey();
            List<Entity> entities = entry.getValue();

            Integer allowed = pluginConfig.getResolvedEntityLimit(type);
            if (allowed == null) {
                CSLLogger.debug(() ->
                        "No limit found for entity type: %s, skipping".formatted(type.name())
                );
                continue;
            }

            int toRemove = entities.size() - allowed;
            if (toRemove <= 0) continue;

            int size = entities.size();
            for (int i = 0; i < toRemove && i < size; i++) {
                Entity entity = entities.get(i);
                if (shouldSkipRemoval(entity)) continue;
                removalAction.accept(entity);
            }
        }
    }

    private boolean shouldSkipRemoval(final Entity entity) {
        // Return false (skip removal) if any preservation check passes
        return Checks.hasCustomName(entity) || Checks.hasMetaData(entity) || ExternalChecks.hasNbtData(entity) || Checks.isPartOfRaid(entity);
    }


    private record QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
    }

    private record DelayedQueuedCheck(Consumer<Entity> action, long timestamp) {
    }


}
