package com.cyprias.chunkspawnerlimiter.listeners;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.cyprias.chunkspawnerlimiter.configs.CslConfig;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Spawn Reasons at <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html">CreatureSpawnEvent.SpawnReason</a>
 */
public class EntityListener implements Listener {
    private final CslConfig config;

    public EntityListener(final ChunkSpawnerLimiter plugin) {
        this.config = plugin.getCslConfig();
    }

    @EventHandler
    public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {
        if (event.isCancelled() || !config.isWatchCreatureSpawns())
            return;

        final String reason = event.getSpawnReason().toString();

        if (!config.isSpawnReason(reason)) {
            ChatUtil.debug(Debug.IGNORE_ENTITY, event.getEntity().getType(), reason);
            return;
        }

        Chunk chunk = event.getLocation().getChunk();
        WorldListener.checkChunk(chunk);
        checkSurroundings(chunk);
    }


    @EventHandler
    public void onVehicleCreateEvent(@NotNull VehicleCreateEvent event) {
        if (event.isCancelled() || !config.isWatchVehicleCreate())
            return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        WorldListener.checkChunk(chunk);
        checkSurroundings(chunk);
    }


    private void checkSurroundings(Chunk chunk) {
        int surrounding = config.getCheckSurroundingChunks();
        if (surrounding > 0) {
            for (int x = chunk.getX() + surrounding; x >= (chunk.getX() - surrounding); x--) {
                for (int z = chunk.getZ() + surrounding; z >= (chunk.getZ() - surrounding); z--) {
                    WorldListener.checkChunk(chunk.getWorld().getChunkAt(x, z));
                }
            }
        }
    }
}
