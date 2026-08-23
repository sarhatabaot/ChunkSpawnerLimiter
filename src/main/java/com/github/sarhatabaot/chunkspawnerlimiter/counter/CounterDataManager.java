package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CounterDataManager {
    private final Map<ChunkCoord, CounterData> loadedChunkCounters = new ConcurrentHashMap<>();

    public CounterData getCounterData(final ChunkCoord chunkCoord) {
        return loadedChunkCounters.computeIfAbsent(chunkCoord, k -> new CounterData());
    }

    public void removeCounterData(final ChunkCoord chunkCoord) {
        loadedChunkCounters.remove(chunkCoord);
    }

    /**
     * Decrements the entity counter for the entity's current chunk.
     * <p>
     * This must be called from every code-path that removes an entity without
     * firing {@link org.bukkit.event.entity.EntityDeathEvent} (e.g. direct
     * {@code entity.remove()} calls), so the cached counters stay correct.
     *
     * @param entity the entity being removed
     */
    public void decrementEntityForRemoval(@NotNull Entity entity) {
        final ChunkCoord coord = ChunkCoord.from(entity);
        final CounterData counterData = getCounterData(coord);
        final EntityType type = entity.getType();
        counterData.decrementEntity(type);
        CSLLogger.debug(() -> "Decremented counter for %s in %s due to plugin removal"
                .formatted(type.name(), coord));
    }

    /**
     * Rebuilds entity counters for all loaded chunks in all worlds by scanning
     * the actual entities present. This is a full cache resync operation.
     *
     * @param isTracked predicate returning {@code true} for entity types the plugin tracks
     * @return number of chunks that were rescanned
     */
    public int rescanAllLoadedChunks(Predicate<EntityType> isTracked) {
        int chunksScanned = 0;
        int entitiesCounted = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                final ChunkCoord coord = ChunkCoord.from(chunk);
                final CounterData counterData = getCounterData(coord);

                // Reset all entity counts for tracked types
                for (EntityType type : counterData.getTrackedEntityTypes()) {
                    counterData.setEntityCount(type, 0);
                }

                // Recount from actual chunk state
                for (Entity entity : chunk.getEntities()) {
                    final EntityType type = entity.getType();
                    if (isTracked.test(type)) {
                        counterData.incrementEntity(type);
                        entitiesCounted++;
                    }
                }
                chunksScanned++;
            }
        }

        final int rescanned = chunksScanned;
        final int counted = entitiesCounted;
        CSLLogger.debug(() -> "Rescanned %d chunks, counted %d tracked entities"
                .formatted(rescanned, counted));
        return chunksScanned;
    }
}