package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class ChunkListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final RemovalTaskManager removalTaskManager;

    public ChunkListener(PluginConfig pluginConfig, CounterDataManager counterDataManager, RemovalTaskManager removalTaskManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        this.removalTaskManager = removalTaskManager;
    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        if (!pluginConfig.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        final Chunk chunk = event.getChunk();
        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);

        addEntityLimits(chunk, chunkCoord);
        addBlockLimits(chunk, chunkCoord);

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalTaskManager.queueChunkCheck(chunkCoord, removalMode.getEntityRemovalAction());
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

    private int chunkCoordsToCoords(int coord) {
        return coord * 16;
    }

    private void addBlockLimits(final @NotNull Chunk chunk, final ChunkCoord chunkCoord) {
        World world = chunk.getWorld();

        int startX = chunkCoordsToCoords(chunk.getX());
        int startZ = chunkCoordsToCoords(chunk.getZ());

        int minY = getWorldMinHeightSafe(world);
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = world.getBlockAt(startX + x, y, startZ + z);
                    if (pluginConfig.hasBlockLimit(block.getType().name())) {
                        counterDataManager.getCounterData(chunkCoord).incrementBlock(block.getType());
                    }
                }
            }
        }
    }

    private static int getWorldMinHeightSafe(World world) {
        try {
            // Check if getMinHeight() exists (MC 1.18+)
            return (int) World.class.getMethod("getMinHeight").invoke(world);
        } catch (Exception ignored) {
            // Older versions (like 1.8.8) start at Y = 0
            return 0;
        }
    }

}
