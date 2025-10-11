package com.github.sarhatabaot.chunkspawnerlimiter.listener;


import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;
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

        if (isUnderOrEqualToLimit(counterData.getBlockCount(material),  pluginConfig.getBlockLimits().get(material.name()))) {
            counterData.incrementBlock(material);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleBlock(event.getBlock(), event);
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        counterDataManager.getCounterData(chunkCoord).decrementBlock(event.getBlock().getType());
    }

    @EventHandler
    public void onEntitySpawn(@NotNull EntitySpawnEvent event) {
        if (!pluginConfig.hasEntityLimit(event.getEntity().getType().name())) { //todo add group (instance of)
            return;
        }

        final EntityType entityType = event.getEntity().getType();
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getEntity().getWorld().getChunkAt(event.getEntity().getLocation()));
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (isUnderOrEqualToLimit(counterData.getEntityCount(entityType), pluginConfig.getEntityLimits().get(entityType.name()))) {
            counterData.incrementEntity(entityType);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(event.getEntity(), event);
    }

// TODO Check that impl works across versions
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

    private boolean isUnderOrEqualToLimit(int count, int limit) {
        return count + 1 <= limit;
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

        if (isUnderOrEqualToLimit(counterData.getEntityCount(vehicleType), pluginConfig.getEntityLimits().get(vehicleType.name()))) {
            counterData.incrementEntity(vehicleType);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(event.getVehicle(), null);
    }

    @EventHandler
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        final ChunkCoord chunkCoord = ChunkCoord.from(event.getVehicle().getWorld().getChunkAt(event.getVehicle().getLocation()));
        counterDataManager.getCounterData(chunkCoord).decrementEntity(event.getVehicle().getType());
    }


}
