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

import java.util.Iterator;
import java.util.Map;
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

    private final Map<UUID, ChunkCoord> entityChunks = new ConcurrentHashMap<>();
    private final CounterDataManager counterDataManager;
    private final Predicate<EntityType> isTracked;
    private final Plugin plugin;
    private final long intervalTicks;

    public EntityChunkTracker(Plugin plugin,
                              CounterDataManager counterDataManager,
                              Predicate<EntityType> isTracked,
                              long intervalTicks) {
        this.plugin = plugin;
        this.counterDataManager = counterDataManager;
        this.isTracked = isTracked;
        this.intervalTicks = intervalTicks;
        startPolling();
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
        entityChunks.put(entity.getUniqueId(), ChunkCoord.from(entity));
    }

    /**
     * Remove tracking for an entity (call on death/despawn/world-exit).
     */
    public void recordExit(@NotNull Entity entity) {
        entityChunks.remove(entity.getUniqueId());
    }

    /**
     * Called periodically to check if any tracked entities have changed chunks
     * and update counters accordingly.
     */
    private void pollEntityMovements() {
        int movesDetected = 0;

        for (Iterator<Map.Entry<UUID, ChunkCoord>> it = entityChunks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, ChunkCoord> entry = it.next();
            UUID uuid = entry.getKey();
            ChunkCoord oldCoord = entry.getValue();

            Entity entity = findEntity(uuid);
            if (entity == null || !entity.isValid()) {
                // Entity no longer exists — remove tracking
                it.remove();
                continue;
            }

            ChunkCoord newCoord = ChunkCoord.from(entity);
            if (!newCoord.equals(oldCoord)) {
                // Entity moved to a different chunk
                counterDataManager.getCounterData(oldCoord).decrementEntity(entity.getType());
                counterDataManager.getCounterData(newCoord).incrementEntity(entity.getType());
                entry.setValue(newCoord);
                movesDetected++;
            }
        }

        if (movesDetected > 0) {
            final int finalMoves = movesDetected;
            CSLLogger.debug(() -> "EntityChunkTracker: detected %d chunk-crossing moves"
                    .formatted(finalMoves));
        }
    }

    /**
     * Find an entity by UUID across all worlds. Compatible with 1.8+.
     */
    private Entity findEntity(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(uuid)) return e;
            }
        }
        return null;
    }

    /**
     * Returns the number of entities currently tracked.
     */
    public int getTrackedCount() {
        return entityChunks.size();
    }
}