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
import java.util.function.LongSupplier;

public class RemovalTaskManager {
    private final static long TICKS_PER_SECOND = 20L;
    private final Queue<QueuedCheck> pendingChunks = new ConcurrentLinkedQueue<>();
    private final Set<ChunkCoord> queuedChunks =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Map of chunks that should be rechecked after a delay (timestamp in ms)
    private final Map<ChunkCoord, ScheduledRecheck> scheduledRechecks = new ConcurrentHashMap<>();

    private final CounterDataManager counterDataManager;
    private final ChunkSpawnerLimiter plugin;
    private final PluginConfig pluginConfig;
    private final LongSupplier currentTimeMillis;
    @Nullable
    private final NmsEntityCounter nmsEntityCounter;

    public RemovalTaskManager(ChunkSpawnerLimiter plugin, CounterDataManager counterDataManager, PluginConfig pluginConfig) {
        this(plugin, counterDataManager, pluginConfig, System::currentTimeMillis, true);
    }

    RemovalTaskManager(ChunkSpawnerLimiter plugin, CounterDataManager counterDataManager,
                       PluginConfig pluginConfig, LongSupplier currentTimeMillis,
                       boolean startProcessing) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.pluginConfig = pluginConfig;
        this.currentTimeMillis = currentTimeMillis;
        this.nmsEntityCounter = NmsEntityCounter.create(pluginConfig);
        if (startProcessing) {
            startProcessingTask();
        }
    }

    public CounterDataManager getCounterDataManager() {
        return counterDataManager;
    }

    /**
     * Schedule this chunk to be checked again after X seconds.
     */
    public void scheduleRecheck(ChunkCoord coord, Consumer<Entity> action, long delaySeconds) {
        long intervalMillis = Math.max(1L, delaySeconds) * 1000L;
        long nextCheck = currentTimeMillis.getAsLong() + intervalMillis;
        scheduledRechecks.put(coord, new ScheduledRecheck(action, intervalMillis, nextCheck));
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

    void processQueue() {
        long now = currentTimeMillis.getAsLong();
        for (Map.Entry<ChunkCoord, ScheduledRecheck> entry : scheduledRechecks.entrySet()) {
            if (shouldPurgeScheduled(entry.getKey())) {
                scheduledRechecks.remove(entry.getKey(), entry.getValue());
                continue;
            }

            ScheduledRecheck scheduled = entry.getValue();
            if (scheduled.nextCheckAt <= now) {
                queueChunkCheck(entry.getKey(), scheduled.action);
                scheduledRechecks.replace(entry.getKey(), scheduled, scheduled.next(now));
            }
        }

        int maxChunks = Math.max(1, pluginConfig.getInspectionMaxChunksPerTick());
        for (int processed = 0; processed < maxChunks; processed++) {
            QueuedCheck check = pendingChunks.poll();
            if (check == null) {
                break;
            }

            try {
                processChunk(check.coord, check.action);
            } finally {
                queuedChunks.remove(check.coord);
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
        Set<EntityType> configuredTypes = new HashSet<>(pluginConfig.getResolvedEntityTypes());
        Set<EntityType> counterTypes = new HashSet<>(data.getTrackedEntityTypes());
        counterTypes.addAll(configuredTypes);
        boolean removalAttempted = false;
        for (EntityType type : counterTypes) {
            data.setEntityCount(type, 0);
        }

        // Recount tracked entities from the actual chunk
        Entity[] entities = chunk.getEntities();
        for (Entity entity : entities) {
            EntityType type = entity.getType();
            if (isCountableEntity(entity)) {
                data.incrementEntity(type);
            }
        }

        // --- Phase 2: Remove excess entities ---
        // Only gather entity lists for types that are actually over the limit
        for (EntityType type : configuredTypes) {
            Integer allowed = pluginConfig.getResolvedEntityLimit(type);
            if (allowed == null) continue;

            int actualCount = data.getEntityCount(type);
            int toRemove = actualCount - allowed;
            if (toRemove <= 0) continue;

            // Collect entities of this type (only when we know we need to remove some)
            List<Entity> typedEntities = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity.getType() == type && isCountableEntity(entity)
                        && !shouldSkipRemoval(entity)) {
                    typedEntities.add(entity);
                }
            }

            int size = typedEntities.size();
            for (int i = 0; i < toRemove && i < size; i++) {
                Entity entity = typedEntities.get(i);
                removalAction.accept(entity);
                removalAttempted = true;
            }
        }

        if (removalAttempted) {
            for (EntityType type : counterTypes) {
                data.setEntityCount(type, 0);
            }

            for (Entity entity : chunk.getEntities()) {
                if (isCountableEntity(entity)) {
                    data.incrementEntity(entity.getType());
                }
            }
        }
    }

    private boolean isCountableEntity(Entity entity) {
        return entity.isValid() && !entity.isDead() && Checks.shouldTrackEntity(entity, pluginConfig);
    }

    private boolean shouldSkipRemoval(final Entity entity) {
        return Checks.hasCustomName(entity) || Checks.hasMetaData(entity) || ExternalChecks.hasNbtData(entity) || Checks.isPartOfRaid(entity);
    }


    private record QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
    }

    private record ScheduledRecheck(Consumer<Entity> action, long intervalMillis, long nextCheckAt) {
        private ScheduledRecheck next(long now) {
            return new ScheduledRecheck(action, intervalMillis, now + intervalMillis);
        }
    }


}
