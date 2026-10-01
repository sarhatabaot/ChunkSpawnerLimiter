package com.github.sarhatabaot.chunkspawnerlimiter.tracker;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Tracks which chunk each entity is in so that when an entity moves between
 * chunks, the counter cache can be updated accordingly.
 * <p>
 * A repeating task polls tracked entities every few seconds to detect chunk
 * changes.
 */
public class EntityChunkTracker {
    private static final int DEFAULT_MAX_ENTITIES_PER_POLL = 256;

    private final Map<UUID, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();
    private final Queue<UUID> pollingQueue = new ConcurrentLinkedQueue<>();
    private final CounterDataManager counterDataManager;
    private final Predicate<Entity> isTracked;
    private final Plugin plugin;
    private final long intervalTicks;
    private final int maxEntitiesPerPoll;

    public EntityChunkTracker(Plugin plugin,
                              CounterDataManager counterDataManager,
                              Predicate<Entity> isTracked,
                              long intervalTicks) {
        this(plugin, counterDataManager, isTracked, intervalTicks,
                DEFAULT_MAX_ENTITIES_PER_POLL, true);
    }

    EntityChunkTracker(Plugin plugin,
                       CounterDataManager counterDataManager,
                       Predicate<Entity> isTracked,
                       long intervalTicks,
                       boolean startPolling) {
        this(plugin, counterDataManager, isTracked, intervalTicks,
                DEFAULT_MAX_ENTITIES_PER_POLL, startPolling);
    }

    EntityChunkTracker(Plugin plugin,
                       CounterDataManager counterDataManager,
                       Predicate<Entity> isTracked,
                       long intervalTicks,
                       int maxEntitiesPerPoll,
                       boolean startPolling) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.isTracked = isTracked;
        this.intervalTicks = intervalTicks;
        this.maxEntitiesPerPoll = Math.max(1, maxEntitiesPerPoll);
        if (startPolling) {
            startPolling();
        }
    }

    private void startPolling() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::pollEntityMovements,
                intervalTicks, intervalTicks);
    }

    /**
     * Record that an entity exists in a specific chunk (call on spawn/world-entry).
     */
    public void recordEntry(@NotNull Entity entity) {
        if (!isTracked.test(entity)) return;
        UUID uuid = entity.getUniqueId();
        if (trackedEntities.put(uuid, TrackedEntity.from(entity)) == null) {
            pollingQueue.offer(uuid);
        }
    }

    /**
     * Remove tracking for an entity (call on death/despawn/world-exit).
     */
    public void recordExit(@NotNull Entity entity) {
        UUID uuid = entity.getUniqueId();
        trackedEntities.remove(uuid);
        while (pollingQueue.remove(uuid)) {
        }
    }

    public void forgetChunk(@NotNull ChunkCoord chunkCoord) {
        for (Map.Entry<UUID, TrackedEntity> entry : trackedEntities.entrySet()) {
            if (entry.getValue().chunkCoord().equals(chunkCoord)
                    && trackedEntities.remove(entry.getKey(), entry.getValue())) {
                while (pollingQueue.remove(entry.getKey())) {
                }
            }
        }
    }

    /**
     * Called periodically to check if any tracked entities have changed chunks
     * and update counters accordingly.
     */
    void pollEntityMovements() {
        int processed = 0;
        while (processed < maxEntitiesPerPoll) {
            UUID uuid = pollingQueue.poll();
            if (uuid == null) {
                break;
            }

            TrackedEntity previous = trackedEntities.get(uuid);
            if (previous != null && reconcileTrackedEntity(uuid, previous)) {
                pollingQueue.offer(uuid);
            }
            processed++;
        }
    }

    private boolean reconcileTrackedEntity(UUID uuid, TrackedEntity previous) {
        Entity entity = previous.entity();
        if (!entity.isValid() || entity.isDead()) {
            if (trackedEntities.remove(uuid, previous)) {
                counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
            }
            return false;
        }

        if (!isTracked.test(entity)) {
            counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
            trackedEntities.remove(uuid, previous);
            return false;
        }

        EntityType currentType = entity.getType();
        ChunkCoord currentCoord = ChunkCoord.from(entity);
        if (previous.type() != currentType || !previous.chunkCoord().equals(currentCoord)) {
            counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
            counterDataManager.getCounterData(currentCoord).incrementEntity(currentType);
            trackedEntities.put(uuid, new TrackedEntity(currentCoord, currentType, entity));
        }
        return true;
    }

    void reconcileEntities(Iterable<Entity> currentEntities) {
        int movesDetected = 0;
        Set<UUID> presentEntities = new HashSet<>();

        for (Entity entity : currentEntities) {
            UUID uuid = entity.getUniqueId();
            TrackedEntity previous = trackedEntities.get(uuid);
            if (previous == null || !entity.isValid()) {
                continue;
            }

            presentEntities.add(uuid);
            EntityType currentType = entity.getType();
            ChunkCoord currentCoord = ChunkCoord.from(entity);

            if (!isTracked.test(entity)) {
                counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
                trackedEntities.remove(uuid, previous);
                movesDetected++;
                continue;
            }

            if (previous.type() != currentType || !previous.chunkCoord().equals(currentCoord)) {
                counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
                counterDataManager.getCounterData(currentCoord).incrementEntity(currentType);
                trackedEntities.put(uuid, new TrackedEntity(currentCoord, currentType, entity));
                movesDetected++;
            }
        }

        int removalsDetected = 0;
        for (Map.Entry<UUID, TrackedEntity> entry : trackedEntities.entrySet()) {
            if (presentEntities.contains(entry.getKey())) {
                continue;
            }

            TrackedEntity removed = entry.getValue();
            if (trackedEntities.remove(entry.getKey(), removed)) {
                counterDataManager.decrementEntityIfPresent(removed.chunkCoord(), removed.type());
                removalsDetected++;
            }
        }

        if (movesDetected > 0 || removalsDetected > 0) {
            final int finalMoves = movesDetected;
            final int finalRemovals = removalsDetected;
            CSLLogger.debug(() -> "EntityChunkTracker: detected %d moves and %d removals"
                    .formatted(finalMoves, finalRemovals));
        }
    }

    /**
     * Returns the number of entities currently tracked.
     */
    public int getTrackedCount() {
        return trackedEntities.size();
    }

    private record TrackedEntity(ChunkCoord chunkCoord, EntityType type, Entity entity) {
        private static TrackedEntity from(Entity entity) {
            return new TrackedEntity(ChunkCoord.from(entity), entity.getType(), entity);
        }
    }
}
