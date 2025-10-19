package com.github.sarhatabaot.chunkspawnerlimiter.listener;


import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
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

import static com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager.isUnderOrEqualToLimit;

public class EventListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;

    public EventListener(PluginConfig pluginConfig, CounterDataManager counterDataManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (pluginConfig.isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }

        if (!pluginConfig.getBlockLimits().containsKey(event.getBlock().getType().name())) {
            CSLLogger.debug("%s block not in block limits.".formatted(event.getBlock().getType().name()));
            return;
        }

        final Material material = event.getBlock().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (isUnderOrEqualToLimit(counterData.getBlockCount(material),  pluginConfig.getBlockLimits().get(material.name()))) {
            CSLLogger.debug("%s block under block limits (%d/%d)".formatted(material.name(), counterData.getBlockCount(material), pluginConfig.getBlockLimits().get(material.name())));
            counterData.incrementBlock(material);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleBlock(event.getBlock(), event);
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (pluginConfig.isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        counterDataManager.getCounterData(chunkCoord).decrementBlock(event.getBlock().getType());
    }

    @EventHandler
    public void onEntitySpawn(@NotNull EntitySpawnEvent event) {
        if (pluginConfig.isWorldDisabled(event.getLocation().getWorld().getName())) {
            return;
        }

        if (!pluginConfig.hasEntityLimit(event.getEntity())) {
            CSLLogger.debug("%s entity not in entity limits.".formatted(event.getEntity().getType().name()));
            return;
        }

        final EntityType entityType = event.getEntity().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (isUnderOrEqualToLimit(counterData.getEntityCount(entityType), pluginConfig.getEntityLimit(event.getEntity()))) {
            CSLLogger.debug("%s entity under entity limits (%d/%d)".formatted(entityType.name(), counterData.getEntityCount(entityType), pluginConfig.getEntityLimit(event.getEntity())));
            counterData.incrementEntity(entityType);
            return;
        }

        //todo impl broadcast to player
        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(event.getEntity(), event);
    }

    private void messagePlayers() {

    }

// TODO Check that impl works across versions, maybe use XMaterial.
// MaterialData was changed at some stage
//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void onPlayerUseEgg(PlayerInteractEvent event) {
//        ItemStack item = event.getItem();
//        if (item != null && item.getType().toString().endsWith("_SPAWN_EGG")) {
//            final EntityType entityType = event.getEntity().getType();
//            final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
//            final CounterData counterData = counterDataManager.getCounterData(chunkCoord);
//
//            if (isUnderOrEqualToLimit(counterData.getEntityCount(entityType), pluginConfig.getEntityLimits().get(entityType.name()))) {
//                counterData.incrementEntity(entityType);
//                return;
//            }
//
//            event.setCancelled(true);
//        }
//    }

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) { //just to decrease counters for tracking.
        if (pluginConfig.isWorldDisabled(event.getEntity().getWorld().getName())) {
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
        counterDataManager.getCounterData(chunkCoord).decrementEntity(event.getEntityType());
    }


    @EventHandler
    public void onVehicleCreate(@NotNull VehicleCreateEvent event) {
        if (pluginConfig.isWorldDisabled(event.getVehicle().getWorld().getName())) {
            return;
        }

        if (!pluginConfig.hasEntityLimit(event.getVehicle())) {
            return;
        }

        final EntityType vehicleType = event.getVehicle().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getVehicle().getWorld().getChunkAt(event.getVehicle().getLocation()));
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (isUnderOrEqualToLimit(counterData.getEntityCount(vehicleType), pluginConfig.getEntityLimit(event.getVehicle()))) {
            counterData.incrementEntity(vehicleType);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(event.getVehicle(), null);
    }

    @EventHandler
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        if (pluginConfig.isWorldDisabled(event.getVehicle().getWorld().getName())) {
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(event.getVehicle().getWorld().getChunkAt(event.getVehicle().getLocation()));
        counterDataManager.getCounterData(chunkCoord).decrementEntity(event.getVehicle().getType());
    }


}
