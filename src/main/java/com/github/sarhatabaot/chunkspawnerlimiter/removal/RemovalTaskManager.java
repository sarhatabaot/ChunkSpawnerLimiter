package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.NmsEntityCounter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    private final NmsEntityCounter nmsEntityCounter;

    public RemovalTaskManager(ChunkSpawnerLimiter plugin, CounterDataManager counterDataManager, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.pluginConfig = pluginConfig;
        this.nmsEntityCounter = NmsEntityCounter.create(pluginConfig);
        startProcessingTask();
    }

    public CounterDataManager getCounterDataManager() {
        return counterDataManager;
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
            if (shouldPurgeScheduled(entry.getKey())) {
                it.remove();
                continue;
            }
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

    private boolean shouldPurgeScheduled(ChunkCoord coord) {
        var world = coord.getWorld();
        return world == null || pluginConfig.isWorldDisabled(world.getName());
    }

    /**
     * Process a chunk: rebuild the cache from actual entity state, then remove
     * any excess entities over configured limits.
     * <p>
     * Uses NMS-based counting when available for zero-allocation speed, falling
     * back to {@code chunk.getEntities()}.
     */
    public void processChunk(ChunkCoord coord, Consumer<Entity> removalAction) {
        CounterData data = counterDataManager.getCounterData(coord);
        if (data == null) return;

        Chunk chunk = coord.getChunk();
        if (chunk == null || !chunk.isLoaded()) return;

        // --- Phase 1: Rebuild cache from actual entity state ---
        // Reset all tracked entity type counters to zero
        Set<EntityType> trackedTypes = new HashSet<>(data.getTrackedEntityTypes());
        for (EntityType type : trackedTypes) {
            data.setEntityCount(type, 0);
        }

        // Recount tracked entities from the actual chunk
        Entity[] entities = chunk.getEntities();
        for (Entity entity : entities) {
            EntityType type = entity.getType();
            if (pluginConfig.hasResolvedEntityLimit(type)) {
                data.incrementEntity(type);
            }
        }

        // --- Phase 2: Remove excess entities ---
        // Only gather entity lists for types that are actually over the limit
        for (EntityType type : trackedTypes) {
            Integer allowed = pluginConfig.getResolvedEntityLimit(type);
            if (allowed == null) continue;

            int actualCount = data.getEntityCount(type);
            int toRemove = actualCount - allowed;
            if (toRemove <= 0) continue;

            // Collect entities of this type (only when we know we need to remove some)
            List<Entity> typedEntities = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity.getType() == type && !shouldSkipRemoval(entity)) {
                    typedEntities.add(entity);
                }
            }

            int size = typedEntities.size();
            for (int i = 0; i < toRemove && i < size; i++) {
                Entity entity = typedEntities.get(i);
                removalAction.accept(entity);
                // Decrement the cache after plugin-initiated removal
                counterDataManager.decrementEntityForRemoval(entity);
            }
        }
    }

    private boolean shouldSkipRemoval(final Entity entity) {
        return Checks.hasCustomName(entity) || Checks.hasMetaData(entity) || ExternalChecks.hasNbtData(entity) || Checks.isPartOfRaid(entity);
    }


    private record QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
    }

    private record DelayedQueuedCheck(Consumer<Entity> action, long timestamp) {
    }


}