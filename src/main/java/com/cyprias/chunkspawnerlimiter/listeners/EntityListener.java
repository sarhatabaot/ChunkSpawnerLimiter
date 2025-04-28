package com.cyprias.chunkspawnerlimiter.listeners;

import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityChunkInspectorScheduler;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.messages.Debug;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import com.cyprias.chunkspawnerlimiter.configs.impl.CslConfig;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Spawn Reasons at <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html">CreatureSpawnEvent.SpawnReason</a>
 */
public class EntityListener implements Listener {
    private final CslConfig config;
    private final EntityChunkInspectorScheduler entityChunkInspectorScheduler;

    public EntityListener(CslConfig config, EntityChunkInspectorScheduler entityChunkInspectorScheduler) {
        this.config = config;
        this.entityChunkInspectorScheduler = entityChunkInspectorScheduler;
    }

    @EventHandler
    public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {
        if (event.isCancelled() || !config.isWatchCreatureSpawns()) {
            return;
        }

        final String reason = event.getSpawnReason().toString();

        if (!config.isSpawnReason(reason)) {
            ChatUtil.debug(Debug.IGNORE_ENTITY, event.getEntity().getType(), reason);
            return;
        }

        final Chunk chunk = event.getLocation().getChunk();
        entityChunkInspectorScheduler.scheduleInspection(chunk,false);
        checkSurroundings(chunk);
    }


    @EventHandler
    public void onVehicleCreateEvent(@NotNull VehicleCreateEvent event) {
        if (event.isCancelled() || !config.isWatchVehicleCreate()) {
            return;
        }

        final Chunk chunk = event.getVehicle().getLocation().getChunk();

        ChatUtil.debug(Debug.VEHICLE_CREATE_EVENT, chunk.getX(), chunk.getZ());
        entityChunkInspectorScheduler.scheduleInspection(chunk,false);
        checkSurroundings(chunk);
    }

    @EventHandler
    public void onEntitySpawnEvent(@NotNull EntitySpawnEvent event) {
        if (event.isCancelled() || event instanceof CreatureSpawnEvent || !config.isWatchEntitySpawns()) {
            return;
        }

        final Chunk chunk = event.getEntity().getLocation().getChunk();

        ChatUtil.debug("Entity Spawn Event: %s, %dx, %dz ", event.getEntity().getType().name(), chunk.getX(), chunk.getZ());
        entityChunkInspectorScheduler.scheduleInspection(chunk,false);
        checkSurroundings(chunk);
    }


    private void checkSurroundings(Chunk chunk) {
        int surrounding = config.getCheckSurroundingChunks();
        if (surrounding > 0) {
            for (int x = chunk.getX() + surrounding; x >= (chunk.getX() - surrounding); x--) {
                for (int z = chunk.getZ() + surrounding; z >= (chunk.getZ() - surrounding); z--) {
                    entityChunkInspectorScheduler.scheduleInspection(chunk.getWorld().getChunkAt(x, z),false);
                }
            }
        }
    }
}
