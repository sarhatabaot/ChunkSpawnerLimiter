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
    private final Map<QueuedCheck, Long> scheduledRechecks = new ConcurrentHashMap<>();

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
        final QueuedCheck check = new QueuedCheck(coord, action);
        scheduledRechecks.put(check, nextCheck);
        CSLLogger.debug(() -> "Scheduled recheck for chunk %s in %d seconds".formatted(coord, delaySeconds));
    }


    public void queueChunkCheck(ChunkCoord coord, Consumer<Entity> action) {
        if (queuedChunks.add(coord)) {
            pendingChunks.add(new QueuedCheck(coord, action));
        }
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
        for (Iterator<Map.Entry<QueuedCheck, Long>> it = scheduledRechecks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<QueuedCheck, Long> entry = it.next();
            if (entry.getValue() <= now) {
                it.remove();
                queueChunkCheck(entry.getKey().coord, entry.getKey().action);
            }
        }
    }

    public void processChunk(ChunkCoord coord, Consumer<Entity> removalAction) {
        CounterData data = counterDataManager.getCounterData(coord);
        if (data == null) return;

        Chunk chunk = coord.getChunk();
        if (chunk == null || !chunk.isLoaded()) {
            return;
        }
        
        // Check entity type limits
        for (EntityType type : data.getTrackedEntityTypes()) {
            List<Entity> entities = Arrays.stream(chunk.getEntities())
                    .filter(e -> e.getType() == type)
                    .toList();

            Integer allowed = pluginConfig.getEntityLimit(type);
            if (allowed == null) {
                CSLLogger.debug(() -> "No limit found for entity type: %s, skipping".formatted(type.name()));
                continue;
            }

            if (entities.size() > allowed) {
                int toRemove = entities.size() - allowed;
                for (int i = 0; i < toRemove; i++) {
                    final Entity entity = entities.get(i);
                    if (shouldSkipRemoval(entity)) {
                        continue;
                    }

                    removalAction.accept(entity);
                    //todo message players, or queue up a task to message players?, I don't want it to block this method
                }
            }
        }
        
        // Check entity group limits
        for (String group : data.getTrackedEntityGroups()) {
            Integer allowed = pluginConfig.getEntityGroupLimit(group);
            if (allowed == null) {
                continue;
            }
            
            int currentCount = data.getEntityGroupCount(group);
            if (currentCount > allowed) {
                // Find entities in this group that exceed the limit
                List<Entity> groupEntities = Arrays.stream(chunk.getEntities())
                        .filter(e -> {
                            String entityGroup = pluginConfig.getEntityGroup(e);
                            return entityGroup != null && group.equals(entityGroup);
                        })
                        .toList();
                
                int toRemove = groupEntities.size() - allowed;
                for (int i = 0; i < toRemove && i < groupEntities.size(); i++) {
                    final Entity entity = groupEntities.get(i);
                    if (shouldSkipRemoval(entity)) {
                        continue;
                    }
                    removalAction.accept(entity);
                }
            }
        }
    }

    private boolean shouldSkipRemoval(final Entity entity) {
        // Return false (skip removal) if any preservation check passes
        return Checks.hasCustomName(entity) || Checks.hasMetaData(entity) || ExternalChecks.hasNbtData(entity) || Checks.isPartOfRaid(entity);
    }


    private record QueuedCheck(ChunkCoord coord, Consumer<Entity> action) {
    }


}
