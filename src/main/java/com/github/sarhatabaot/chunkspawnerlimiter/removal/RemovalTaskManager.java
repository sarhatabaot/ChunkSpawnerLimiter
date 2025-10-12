package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RemovalTaskManager {
    private final Queue<QueuedCheck> pendingChunks = new ConcurrentLinkedQueue<>();
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
        pendingChunks.add(new QueuedCheck(coord, action));
    }


    private void startProcessingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 20L, 20L); // every 1 second
    }

    private void processQueue() {
        QueuedCheck check;
        while ((check = pendingChunks.poll()) != null) {
            processChunk(check.coord, check.action);
        }
    }


    private final Consumer<Entity> defaultRemoval = Entity::remove;

    private void processChunk(ChunkCoord coord) {
        processChunk(coord, defaultRemoval);
    }

    public void processChunk(ChunkCoord coord, Consumer<Entity> removalAction) {
        CounterData data = counterDataManager.getCounterData(coord);
        if (data == null) return;

        Chunk chunk = coord.getChunk();
        for (EntityType type : data.getTrackedEntityTypes()) {
            List<Entity> entities = Arrays.stream(chunk.getEntities())
                    .filter(e -> e.getType() == type)
                    .toList();

            int allowed = pluginConfig.getEntityLimits().get(type.name());
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
