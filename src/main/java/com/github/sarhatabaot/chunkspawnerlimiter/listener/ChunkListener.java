package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.BlockScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.BlockScannerFactory;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Chunk;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;


public class ChunkListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final RemovalTaskManager removalTaskManager;
    private final BlockScanner blockScanner;

    public ChunkListener(Plugin plugin, PluginConfig pluginConfig, CounterDataManager counterDataManager, RemovalTaskManager removalTaskManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        this.removalTaskManager = removalTaskManager;
        this.blockScanner = BlockScannerFactory.create(plugin, pluginConfig, counterDataManager);
    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        if (pluginConfig.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        final Chunk chunk = event.getChunk();
        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);

        addEntityLimits(chunk, chunkCoord);
        
        // Scan blocks asynchronously on chunk load
        blockScanner.scanChunk(chunk, chunkCoord, true);

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
        removalTaskManager.removeChunkRecheck(chunkCoord);
    }

    private void addEntityLimits(final @NotNull Chunk chunk, final ChunkCoord chunkCoord) {
        final Entity[] entities = chunk.getEntities();
        for (Entity entity: entities) {
            if (Checks.shouldSkipPlayers(entity)) {
                continue;
            }

            if (pluginConfig.hasResolvedEntityLimit(entity.getType())) {
                counterDataManager.getCounterData(chunkCoord).incrementEntity(entity.getType());
            }
        }
    }
}
