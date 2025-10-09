package com.github.sarhatabaot.chunkspawnerlimiter.listener;


import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.jetbrains.annotations.NotNull;

public class EventListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;

    public EventListener(PluginConfig pluginConfig, CounterDataManager counterDataManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (!pluginConfig.getBlockLimits().containsKey(event.getBlock().getType().name())) {
            return;
        }

        final Material material = event.getBlock().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (counterData.getBlockCount(material) + 1 <= pluginConfig.getBlockLimits().get(material.name())) {
            counterData.incrementBlock(material);
            return;
        }

        //todo mode logic;
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        counterDataManager.getCounterData(chunkCoord).decrementBlock(event.getBlock().getType());
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!pluginConfig.getEntityLimits().containsKey(event.getEntity().getType().name())) {
            return;
        }

        final EntityType entityType = event.getEntity().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (counterData.getEntityCount(entityType) + 1 <= pluginConfig.getEntityLimits().get(entityType.name())) {
            counterData.incrementEntity(entityType);
            return;
        }

        //mode logic, probably in a different class.

        //check if there is a limit. If there isn't return.
        //check if we increase by one it exceeds the limit, if it does, don't increase it, and act according to mode
        //if it doesn't, increase it by one and return.

        //this logic applied to vehicle & block as well.
    }

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) { //just to decrease counters for tracking.
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
        counterDataManager.getCounterData(chunkCoord).decrementEntity(event.getEntityType());
    }

    @EventHandler
    public void onVehicleCreate(@NotNull VehicleCreateEvent event) {
        if (!pluginConfig.getEntityLimits().containsKey(event.getVehicle().getType().name())) {
            return;
        }

        final EntityType vehicleType = event.getVehicle().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getVehicle().getWorld().getChunkAt(event.getVehicle().getLocation()));
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (counterData.getEntityCount(vehicleType) + 1 <= pluginConfig.getEntityLimits().get(vehicleType.name())) {
            counterData.incrementEntity(vehicleType);
            return;
        }

        //mode logic, probably in a different class.
    }

    @EventHandler
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getVehicle().getWorld().getChunkAt(event.getVehicle().getLocation()));
        counterDataManager.getCounterData(chunkCoord).decrementEntity(event.getVehicle().getType());
    }


}
