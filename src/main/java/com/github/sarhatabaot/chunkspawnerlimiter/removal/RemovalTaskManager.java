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

    // Map of chunks that should be rechecked after a delay (timestamp in ms)
    private final Map<ChunkCoord, Long> scheduledRechecks = new ConcurrentHashMap<>();

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
        scheduledRechecks.put(coord, nextCheck);
        CSLLogger.debug("Scheduled recheck for chunk %s in %d seconds".formatted(coord, delaySeconds));
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

        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<ChunkCoord, Long>> it = scheduledRechecks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ChunkCoord, Long> entry = it.next();
            if (entry.getValue() <= now) {
                it.remove();
                queueChunkCheck(entry.getKey(), getDefaultAction());
            }
        }
    }

    private Consumer<Entity> getDefaultAction() {
        return pluginConfig.getRemovalMode().getEntityRemovalAction();
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
                    final Entity entity = entities.get(i);
                    if (!checks(entity)) {
                        continue;
                    }

                    removalAction.accept(entity);
                }
            }
        }
    }

    private boolean checks(final Entity entity) {
        return hasCustomName(entity) || hasMetaData(entity);
    }

    //todo make this not static.
    public static boolean isUnderOrEqualToLimit(int count, int limit) {
        return count + 1 <= limit;
    }

    private record QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
    }

    private boolean hasCustomName(final Entity entity) {
        //check from config
        if (!pluginConfig.shouldPreserveNamedEntities()) {
            return false;
        }
        return entity.getCustomName() != null;
    }

    //todo, I don't really want to add reliance on another library, and if this doesn't allow us to use 1.8.8, it's a no go
    // this will be optional, not required. that way users don't have to use this.
    private boolean hasNbtData(final Entity entity) {
        return false;
    }

    private boolean hasMetaData(final Entity entity) {
        if (pluginConfig.getIgnoreMetadata().isEmpty()) {
            return false;
        }

        for (final String metadata: pluginConfig.getIgnoreMetadata()) {
            if (entity.hasMetadata(metadata))
                return true;
        }
        return false;
    }
}
