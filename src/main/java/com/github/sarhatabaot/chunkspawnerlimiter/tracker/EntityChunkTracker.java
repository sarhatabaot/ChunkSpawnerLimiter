package com.github.sarhatabaot.chunkspawnerlimiter.tracker;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Tracks which chunk each entity is in so that when an entity moves between
 * chunks, the counter cache can be updated accordingly.
 * <p>
 * A repeating task polls tracked entities every few seconds to detect chunk
 * changes.
 */
public class EntityChunkTracker {

    private final Map<UUID, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();
    private final CounterDataManager counterDataManager;
    private final Predicate<EntityType> isTracked;
    private final Plugin plugin;
    private final long intervalTicks;

    public EntityChunkTracker(Plugin plugin,
                              CounterDataManager counterDataManager,
                              Predicate<EntityType> isTracked,
                              long intervalTicks) {
        this(plugin, counterDataManager, isTracked, intervalTicks, true);
    }

    EntityChunkTracker(Plugin plugin,
                       CounterDataManager counterDataManager,
                       Predicate<EntityType> isTracked,
                       long intervalTicks,
                       boolean startPolling) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.isTracked = isTracked;
        this.intervalTicks = intervalTicks;
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
        if (!isTracked.test(entity.getType())) return;
        trackedEntities.put(entity.getUniqueId(), TrackedEntity.from(entity));
    }

    /**
     * Remove tracking for an entity (call on death/despawn/world-exit).
     */
    public void recordExit(@NotNull Entity entity) {
        trackedEntities.remove(entity.getUniqueId());
    }

    public void forgetChunk(@NotNull ChunkCoord chunkCoord) {
        trackedEntities.entrySet().removeIf(entry -> entry.getValue().chunkCoord().equals(chunkCoord));
    }

    /**
     * Called periodically to check if any tracked entities have changed chunks
     * and update counters accordingly.
     */
    void pollEntityMovements() {
        List<Entity> currentEntities = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            currentEntities.addAll(world.getEntities());
        }
        reconcileEntities(currentEntities);
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

            if (previous.type() != currentType || !previous.chunkCoord().equals(currentCoord)) {
                counterDataManager.decrementEntityIfPresent(previous.chunkCoord(), previous.type());
                if (isTracked.test(currentType)) {
                    counterDataManager.getCounterData(currentCoord).incrementEntity(currentType);
                    trackedEntities.put(uuid, new TrackedEntity(currentCoord, currentType));
                } else {
                    trackedEntities.remove(uuid, previous);
                }
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

    private record TrackedEntity(ChunkCoord chunkCoord, EntityType type) {
        private static TrackedEntity from(Entity entity) {
            return new TrackedEntity(ChunkCoord.from(entity), entity.getType());
        }
    }
}
