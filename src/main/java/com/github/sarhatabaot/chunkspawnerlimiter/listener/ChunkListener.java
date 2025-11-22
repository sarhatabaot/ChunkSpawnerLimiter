package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.NmsBlockScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Chunk;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;


public class ChunkListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final RemovalTaskManager removalTaskManager;
    private final NmsBlockScanner nmsBlockScanner;

    public ChunkListener(PluginConfig pluginConfig, CounterDataManager counterDataManager, RemovalTaskManager removalTaskManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        this.removalTaskManager = removalTaskManager;
        try {
            this.nmsBlockScanner = new NmsBlockScanner(pluginConfig, counterDataManager);
        } catch (Exception e) {
            CSLLogger.error("Failed to initialize NMS Block Scanner: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        if (pluginConfig.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        final Chunk chunk = event.getChunk();
        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);

        addEntityLimits(chunk, chunkCoord);
        try {
            this.nmsBlockScanner.scanChunk(chunk, chunkCoord);
        } catch (Exception e) {
            CSLLogger.error("There was a problem trying to add block limits in this chunk.");
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalTaskManager.queueChunkCheck(chunkCoord, removalMode.getEntityRemovalAction());

        if (pluginConfig.isActiveInspections()) {
            removalTaskManager.scheduleRecheck(chunkCoord, removalMode.getEntityRemovalAction(), pluginConfig.getInspectionFrequency());
        }
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        if (pluginConfig.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(event.getChunk());
        counterDataManager.removeCounterData(chunkCoord);
    }

    private void addEntityLimits(final @NotNull Chunk chunk, final ChunkCoord chunkCoord) {
        final Entity[] entities = chunk.getEntities();
        for (Entity entity: entities) {
            if (entity instanceof Player && !pluginConfig.isKillPlayers()) {
                //is player & kill players is disabled
                continue;
            }

            if (pluginConfig.hasEntityLimit(entity.getType().name())) {
                counterDataManager.getCounterData(chunkCoord).incrementEntity(entity.getType());
            }

            final String entityGroup = pluginConfig.getEntityGroup(entity);
            if (pluginConfig.hasEntityLimit(entityGroup)) {
                counterDataManager.getCounterData(chunkCoord).incrementEntityGroup(entityGroup);
            }
        }
    }
}
