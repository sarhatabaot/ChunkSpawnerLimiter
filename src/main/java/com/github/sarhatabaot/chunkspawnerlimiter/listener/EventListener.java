package com.github.sarhatabaot.chunkspawnerlimiter.listener;


import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.jetbrains.annotations.NotNull;


public class EventListener implements Listener {
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final NotificationService notificationService;

    public EventListener(PluginConfig pluginConfig, CounterDataManager counterDataManager, NotificationService notificationService) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        this.notificationService = notificationService;
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (pluginConfig.isWorldDisabled(event.getBlock().getWorld().getName())) {
            CSLLogger.debug(() -> "%s world is disabled.".formatted(event.getBlock().getWorld().getName()));
            return;
        }

        final Material material = event.getBlock().getType();
        if (!pluginConfig.hasResolvedBlockLimit(material)) {
            CSLLogger.debug(() -> "%s block not in block limits.".formatted(material.name()));
            return;
        }


        final ChunkCoord chunkCoord = ChunkCoord.from(event.getBlock().getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        if (Checks.isUnderOrEqualToLimit(counterData.getBlockCount(material),  pluginConfig.getResolvedBlockLimit(material))) {
            CSLLogger.debug(() -> "%s block under block limits (%d/%d)".formatted(material.name(), counterData.getBlockCount(material), pluginConfig.getResolvedBlockLimit(material)));
            counterData.incrementBlock(material);
            return;
        }

        // Notify player about block limit
        notificationService.notifyBlockLimitReached(
            event.getPlayer(), 
            material, 
            pluginConfig.getResolvedBlockLimit(material)
        );

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
            CSLLogger.debug(() -> "%s world is disabled.".formatted(event.getLocation().getWorld().getName()));
            return;
        }

        // Check spawn reason if this is a CreatureSpawnEvent
        if (event instanceof CreatureSpawnEvent creatureSpawnEvent) {
            String spawnReason = creatureSpawnEvent.getSpawnReason().name();
            if (!pluginConfig.getSpawnReasons().contains(spawnReason)) {
                CSLLogger.debug(() -> "%s entity spawn ignored due to spawn reason: %s".formatted(event.getEntity().getType().name(), spawnReason));
                return;
            }
        }

        final Entity entity = event.getEntity();
        final EntityType entityType = entity.getType();
        if (!pluginConfig.hasResolvedEntityLimit(entityType)) {
            CSLLogger.debug(() -> "%s entity not in entity limits.".formatted(entityType.name()));
            return;
        }

        if (Checks.shouldSkipPlayers(entity)) {
            return;
        }

        final Chunk chunk = entity.getLocation().getChunk();
        if (!chunk.isLoaded()) {
            CSLLogger.debug(() -> "Chunk not loaded for entity spawn: %s".formatted(entityType.name()));
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        // Check both entity type and entity group limits
        final Integer entityTypeLimit = pluginConfig.getResolvedEntityLimit(entityType);

        // Check entity type limit
        boolean withinTypeLimit = entityTypeLimit == null || 
            Checks.isUnderOrEqualToLimit(counterData.getEntityCount(entityType), entityTypeLimit);

        if (withinTypeLimit) {
            CSLLogger.debug(() -> "%s entity under entity limits (type: %d/%s)".formatted(
                entityType.name(), 
                counterData.getEntityCount(entityType), 
                entityTypeLimit != null ? String.valueOf(entityTypeLimit) : "unlimited"
            ));
            
            counterData.incrementEntity(entityType);
            return;
        }

        // Notify players in chunk about blocked entity
        notificationService.notifyEntitiesBlocked(chunk, entityType, 1);

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(entity, event);
    }


// TODO Check that impl works across versions, use reflection.
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

        final Entity entity = event.getEntity();
        final ChunkCoord chunkCoord = ChunkCoord.from(entity.getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        counterData.decrementEntity(entity.getType());
    }


    @EventHandler
    public void onVehicleCreate(@NotNull VehicleCreateEvent event) {
        if (pluginConfig.isWorldDisabled(event.getVehicle().getWorld().getName())) {
            return;
        }

        final Entity vehicle = event.getVehicle();
        final EntityType vehicleType = vehicle.getType();
        if (!pluginConfig.hasResolvedEntityLimit(vehicleType)) {
            return;
        }

        final Chunk chunk = vehicle.getLocation().getChunk();
        if (!chunk.isLoaded()) {
            return;
        }


        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        // Check both entity type and entity group limits
        final Integer vehicleTypeLimit = pluginConfig.getResolvedEntityLimit(vehicleType);
        boolean withinTypeLimit = vehicleTypeLimit == null || 
            Checks.isUnderOrEqualToLimit(counterData.getEntityCount(vehicleType), vehicleTypeLimit);

        if (withinTypeLimit) {
            counterData.incrementEntity(vehicleType);
            return;
        }

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(vehicle, null);
    }

    @EventHandler
    public void onVehicleDestroy(@NotNull VehicleDestroyEvent event) {
        if (pluginConfig.isWorldDisabled(event.getVehicle().getWorld().getName())) {
            return;
        }

        final Entity vehicle = event.getVehicle();
        final ChunkCoord chunkCoord = ChunkCoord.from(vehicle.getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        counterData.decrementEntity(vehicle.getType());
    }


}
