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

public class EventListener implements Listener {
    private final Plugin plugin;
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;
    private final NotificationService notificationService;
    private final EntityChunkTracker chunkTracker;

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        if (!Checks.shouldTrackEntity(entity, pluginConfig)) {
            CSLLogger.debug(() -> "%s entity not in entity limits.".formatted(entityType.name()));
            return;
        }

        final Chunk chunk = entity.getLocation().getChunk();
        if (!chunk.isLoaded()) {
            CSLLogger.debug(() -> "Chunk not loaded for entity spawn: %s".formatted(entityType.name()));
            return;
        }

        // Stacking-plugin compatibility: defer the limit check AND the counter
        // update to the next tick. By then, WildStacker/RoseStacker will have
        // merged transient spawn entities into stacked entities, so we count
        // the actual stacks (1 per stack, not 1 per individual entity).
        if (pluginConfig.shouldDelayEntityCountForCompatibility()) {
            scheduleEntityCountFinalization(entity);
            return;
        }

        final ChunkCoord chunkCoord = ChunkCoord.from(chunk);
        final CounterData counterData = counterDataManager.getCounterData(chunkCoord);
        final int existingEntityCount = counterDataManager.synchronizeEntityCount(chunk, entityType, entity);

        final Integer entityTypeLimit = pluginConfig.getResolvedEntityLimit(entityType);

        boolean withinTypeLimit = entityTypeLimit == null ||
            Checks.isUnderOrEqualToLimit(existingEntityCount, entityTypeLimit);

        if (withinTypeLimit) {
            CSLLogger.debug(() -> "%s entity under entity limits (type: %d/%s)".formatted(
                entityType.name(),
                existingEntityCount,
                entityTypeLimit != null ? String.valueOf(entityTypeLimit) : "unlimited"
            ));

            counterData.incrementEntity(entityType);
            // Track entity for cross-chunk movement detection
            chunkTracker.recordEntry(entity);
            return;
        }

        notificationService.notifyEntitiesBlocked(chunk, entityType, 1);

        RemovalMode removalMode = pluginConfig.getRemovalMode();
        removalMode.handleEntity(entity, event);
        // Note: we intentionally do NOT drop a refund egg when the event is cancelled.
        // On modern Paper/Spigot, cancelling CreatureSpawnEvent prevents the entity
        // from spawning, but the player's spawn-egg item has already been consumed
        // from the hand. Dropping a new egg would create an unlimited dupe exploit.
        // (See bug report: "i set the villager limit to 5... i keep my own egg" dupe.)
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPortal(@NotNull EntityPortalEvent event) {
        if (event.isCancelled() || event.getFrom().getWorld() == null) {
            return;
        }

        final Entity entity = event.getEntity();
        if (!Checks.shouldTrackEntity(entity, pluginConfig)) return;

        final ChunkCoord sourceCoord = ChunkCoord.from(event.getFrom());
        final EntityType sourceType = entity.getType();
        final boolean sourceTracked = !pluginConfig.isWorldDisabled(event.getFrom().getWorld().getName());
        plugin.getServer().getScheduler().runTask(plugin,
                () -> reconcilePortalTransition(entity, sourceCoord, sourceType, sourceTracked));
    }

    // -- Entity transformation (pig→zombified piglin, etc.) -----------------

    private void reconcilePortalTransition(@NotNull Entity entity, @NotNull ChunkCoord sourceCoord,
                                           @NotNull EntityType sourceType, boolean sourceTracked) {
        if (!entity.isValid()) {
            if (sourceTracked) {
                counterDataManager.decrementEntityIfPresent(sourceCoord, sourceType);
            }
            chunkTracker.recordExit(entity);
            return;
        }

        final ChunkCoord destinationCoord = ChunkCoord.from(entity);
        final boolean destinationTracked = !pluginConfig.isWorldDisabled(entity.getWorld().getName())
                && Checks.shouldTrackEntity(entity, pluginConfig);

        if (!sourceCoord.equals(destinationCoord)) {
            if (sourceTracked) {
                counterDataManager.decrementEntityIfPresent(sourceCoord, sourceType);
            }
            if (destinationTracked) {
                counterDataManager.getCounterData(destinationCoord).incrementEntity(entity.getType());
            }
        }

        if (destinationTracked) {
            chunkTracker.recordEntry(entity);
        } else {
            chunkTracker.recordExit(entity);
        }

        CSLLogger.debug(() -> "Entity portal: %s moved from %s to %s"
                .formatted(entity.getType().name(), sourceCoord, destinationCoord));
    }

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

    // -- Vehicle events -----------------------------------------------------

    @EventHandler
    public void onVehicleCreate(@NotNull VehicleCreateEvent event) {
        if (pluginConfig.isWorldDisabled(event.getVehicle().getWorld().getName())) {
            return;
        }

        final Entity vehicle = event.getVehicle();
        final EntityType vehicleType = vehicle.getType();
        if (!Checks.shouldTrackEntity(vehicle, pluginConfig)) {
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
            // --- Stale-event guards -------------------------------------------------
            if (!entity.isValid()) {
                return;
            }

            if (pluginConfig.isWorldDisabled(entity.getWorld().getName())) {
                return;
            }

            if (!Checks.shouldTrackEntity(entity, pluginConfig)) {
                return;
            }

            final Chunk chunk = entity.getLocation().getChunk();
            if (!chunk.isLoaded()) {
                return;
            }

            // --- Stacking-plugin aware limit enforcement -----------------------------
            // Count actual entities in the chunk (each WildStacker stack is 1 entity),
            // then either sync the counter (under limit) or remove excess (over limit).
            final ChunkCoord chunkCoord = ChunkCoord.from(chunk);
            final CounterData counterData = counterDataManager.getCounterData(chunkCoord);
            final EntityType entityType = entity.getType();
            final Integer entityTypeLimit = pluginConfig.getResolvedEntityLimit(entityType);

            int actualCount = 0;
            for (Entity e : chunk.getEntities()) {
                if (e.getType() == entityType && e.isValid() && !e.isDead()) actualCount++;
            }

            // Sync counter from actual chunk state (each stack = 1, not N).
            counterData.setEntityCount(entityType, actualCount);

            if (entityTypeLimit != null && actualCount > entityTypeLimit) {
                int toRemove = actualCount - entityTypeLimit;
                final int fActualCount = actualCount;
                CSLLogger.debug(() -> "Stacking compat: chunk %s has %d %s, limit %d, removing %d"
                        .formatted(chunkCoord, fActualCount, entityType.name(), entityTypeLimit, toRemove));

                int removed = 0;
                for (Entity e : chunk.getEntities()) {
                    if (removed >= toRemove) break;
                    if (e.getType() != entityType) continue;
                    // Prefer to remove the entity that just spawned (the one this
                    // deferred task is for) so existing stacks remain intact.
                    if (e.equals(entity)) {
                        RemovalMode removalMode = pluginConfig.getRemovalMode();
                        removalMode.handleDeferredEntity(e);
                        if (!e.isValid() || e.isDead()) {
                            actualCount = Math.max(0, actualCount - 1);
                            counterData.setEntityCount(entityType, actualCount);
                            removed++;
                        }
                    } else if (e.getTicksLived() < 5) {
                        // Recent spawn — likely a transient pre-merge entity. Remove
                        // to enforce the limit without disturbing existing stacks.
                        e.remove();
                        if (!e.isValid() || e.isDead()) {
                            counterData.decrementEntity(entityType);
                            actualCount = Math.max(0, actualCount - 1);
                            chunkTracker.recordExit(e);
                            removed++;
                        }
                    }
                }
            }

            if (entity.isValid() && !entity.isDead()) {
                chunkTracker.recordEntry(entity);
            } else {
                chunkTracker.recordExit(entity);
            }
        });
    }

}
