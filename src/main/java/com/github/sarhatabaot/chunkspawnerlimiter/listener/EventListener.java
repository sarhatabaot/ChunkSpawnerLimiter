package com.github.sarhatabaot.chunkspawnerlimiter.listener;


import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import com.github.sarhatabaot.chunkspawnerlimiter.util.SpawnEggUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;


public class EventListener implements Listener {
    private final Plugin plugin;
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final NotificationService notificationService;
    private final EntityChunkTracker chunkTracker;

    // Paper-only EntityTransformEvent support (1.19+)
    private static final boolean HAS_ENTITY_TRANSFORM_EVENT;
    private static final Method ENTITY_TRANSFORM_GET_TRANSFORMED_ENTITY;

    static {
        boolean hasTransform = false;
        Method getTransformed = null;
        try {
            Class<?> transformEvent = Class.forName("com.destroystokyo.paper.event.entity.EntityTransformEvent");
            getTransformed = transformEvent.getMethod("getTransformedEntity");
            hasTransform = true;
        } catch (Throwable ignored) {}
        HAS_ENTITY_TRANSFORM_EVENT = hasTransform;
        ENTITY_TRANSFORM_GET_TRANSFORMED_ENTITY = getTransformed;
    }

    public EventListener(Plugin plugin, PluginConfig pluginConfig,
                         CounterDataManager counterDataManager,
                         NotificationService notificationService,
                         EntityChunkTracker chunkTracker) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        this.notificationService = notificationService;
        this.chunkTracker = chunkTracker;
    }

    // -- Block events --------------------------------------------------------

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

    // -- Entity spawn events -------------------------------------------------

    @EventHandler
    public void onEntitySpawn(@NotNull EntitySpawnEvent event) {
        if (pluginConfig.isWorldDisabled(event.getLocation().getWorld().getName())) {
            CSLLogger.debug(() -> "%s world is disabled.".formatted(event.getLocation().getWorld().getName()));
            return;
        }

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

        final Integer entityTypeLimit = pluginConfig.getResolvedEntityLimit(entityType);

        boolean withinTypeLimit = entityTypeLimit == null ||
            Checks.isUnderOrEqualToLimit(counterData.getEntityCount(entityType), entityTypeLimit);

        if (withinTypeLimit) {
            CSLLogger.debug(() -> "%s entity under entity limits (type: %d/%s)".formatted(
                entityType.name(),
                counterData.getEntityCount(entityType),
                entityTypeLimit != null ? String.valueOf(entityTypeLimit) : "unlimited"
            ));

            if (pluginConfig.shouldDelayEntityCountForCompatibility()) {
                scheduleEntityCountFinalization(entity);
            } else {
                counterData.incrementEntity(entityType);
            }

            // Track entity for cross-chunk movement detection
            chunkTracker.recordEntry(entity);
            return;
        }

        notificationService.notifyEntitiesBlocked(chunk, entityType, 1);

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(entity, event);

        if (event.isCancelled() && event instanceof CreatureSpawnEvent creatureSpawnEvent) {
            if (SpawnEggUtil.isSpawnEggSpawn(creatureSpawnEvent.getSpawnReason().name())) {
                SpawnEggUtil.dropSpawnEgg(entity.getType(), event.getLocation());
            }
        }
    }

    // -- Entity death / removal events --------------------------------------

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (pluginConfig.isWorldDisabled(event.getEntity().getWorld().getName())) {
            return;
        }

        final Entity entity = event.getEntity();
        final ChunkCoord chunkCoord = ChunkCoord.from(entity.getLocation());
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);

        counterData.decrementEntity(entity.getType());
        chunkTracker.recordExit(entity);
    }

    // -- Entity portal (cross-dimension) ------------------------------------

    @EventHandler
    public void onEntityPortal(@NotNull EntityPortalEvent event) {
        if (pluginConfig.isWorldDisabled(event.getFrom().getWorld().getName())) {
            return;
        }

        final Entity entity = event.getEntity();
        if (!pluginConfig.hasResolvedEntityLimit(entity.getType())) return;

        // Entity is leaving this dimension — decrement its old chunk counter.
        // A new entity will be created in the target world, and its spawn event
        // will increment the counter there.
        final ChunkCoord oldCoord = ChunkCoord.from(entity.getLocation());
        counterDataManager.getCounterData(oldCoord).decrementEntity(entity.getType());
        chunkTracker.recordExit(entity);

        CSLLogger.debug(() -> "Entity portal: %s leaving %s"
                .formatted(entity.getType().name(), oldCoord));
    }

    // -- Entity transformation (pig→zombified piglin, etc.) -----------------

    @EventHandler
    public void onPigZap(@NotNull PigZapEvent event) {
        if (pluginConfig.isWorldDisabled(event.getEntity().getWorld().getName())) {
            return;
        }

        // The pig is being transformed to a zombified piglin.
        // Decrement PIG counter; the new ZOMBIFIED_PIGLIN will fire
        // its own CreatureSpawnEvent (LIGHTNING reason) which we handle.
        final Pig pig = event.getEntity();
        if (pluginConfig.hasResolvedEntityLimit(EntityType.PIG)) {
            final ChunkCoord coord = ChunkCoord.from(pig.getLocation());
            counterDataManager.getCounterData(coord).decrementEntity(EntityType.PIG);
            chunkTracker.recordExit(pig);
            CSLLogger.debug(() -> "Pig zapped in %s".formatted(coord));
        }
    }

    @EventHandler
    public void onEntityTransform(Object event) {
        // Paper 1.19+ EntityTransformEvent — handled via reflection
        if (!HAS_ENTITY_TRANSFORM_EVENT) return;

        try {
            Entity original = (Entity) event.getClass().getMethod("getEntity").invoke(event);
            Entity transformed = (Entity) ENTITY_TRANSFORM_GET_TRANSFORMED_ENTITY.invoke(event);

            if (pluginConfig.isWorldDisabled(original.getWorld().getName())) return;

            EntityType oldType = original.getType();
            EntityType newType = transformed.getType();

            // If the type changed, decrement old and increment new
            if (oldType != newType) {
                final ChunkCoord coord = ChunkCoord.from(original.getLocation());

                if (pluginConfig.hasResolvedEntityLimit(oldType)) {
                    counterDataManager.getCounterData(coord).decrementEntity(oldType);
                }
                chunkTracker.recordExit(original);

                if (pluginConfig.hasResolvedEntityLimit(newType)) {
                    counterDataManager.getCounterData(coord).incrementEntity(newType);
                }
                chunkTracker.recordEntry(transformed);

                CSLLogger.debug(() -> "Entity transform: %s→%s in %s"
                        .formatted(oldType.name(), newType.name(), coord));
            }
        } catch (Throwable t) {
            CSLLogger.debug(() -> "Entity transform handler error: " + t.getMessage());
        }
    }

    // -- Vehicle events -----------------------------------------------------

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

        final Integer vehicleTypeLimit = pluginConfig.getResolvedEntityLimit(vehicleType);
        boolean withinTypeLimit = vehicleTypeLimit == null ||
            Checks.isUnderOrEqualToLimit(counterData.getEntityCount(vehicleType), vehicleTypeLimit);

        if (withinTypeLimit) {
            counterData.incrementEntity(vehicleType);
            chunkTracker.recordEntry(vehicle);
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
        chunkTracker.recordExit(vehicle);
    }

    // -- Internal helpers ---------------------------------------------------

    private void scheduleEntityCountFinalization(@NotNull Entity entity) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!entity.isValid()) {
                return;
            }

            if (pluginConfig.isWorldDisabled(entity.getWorld().getName())) {
                return;
            }

            if (Checks.shouldSkipPlayers(entity) || !pluginConfig.hasResolvedEntityLimit(entity.getType())) {
                return;
            }

            final Chunk chunk = entity.getLocation().getChunk();
            if (!chunk.isLoaded()) {
                return;
            }

            final CounterData counterData = counterDataManager.getCounterData(ChunkCoord.from(chunk));
            counterData.incrementEntity(entity.getType());
            chunkTracker.recordEntry(entity);
        });
    }

}