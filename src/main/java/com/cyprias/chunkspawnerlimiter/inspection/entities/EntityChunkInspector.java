package com.cyprias.chunkspawnerlimiter.inspection.entities;


import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.compare.MobGroupCompare;
import com.cyprias.chunkspawnerlimiter.configs.impl.CslConfig;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Raid;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class EntityChunkInspector {
    private final ChunkSpawnerLimiter plugin;
    private final CslConfig config;

    private final ConcurrentHashMap<ChunkIdentifier, AtomicBoolean> activeChunks = new ConcurrentHashMap<>();

    public EntityChunkInspector(@NotNull ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
        this.config = plugin.getCslConfig();
    }

    /**
     * Checks the chunk for entities, removes entities if over the limit.
     *
     * @param chunk Chunk
     */
    public void checkChunk(@NotNull Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        if (config.isWorldNotAllowed(worldName)) {
            ChatUtil.debug("World %s is not allowed", worldName);
            return;
        }

        ChunkIdentifier chunkId = new ChunkIdentifier(
                chunk.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ()
        );

        // Attempt to add the chunk with a new flag (default: no re-check needed)
        AtomicBoolean needsRecheck = new AtomicBoolean(false);
        AtomicBoolean existingFlag = activeChunks.putIfAbsent(chunkId, needsRecheck);

        if (existingFlag != null) {
            // Chunk is already being processed. Mark for re-check.
            existingFlag.set(true);
            return;
        }

        // Offload calculations to an async task
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Entity[] entities = chunk.getEntities();
                    // Perform calculations async
                    Map<String, ArrayList<Entity>> types = addEntitiesByConfig(entities);
                    List<Entity> entitiesToRemove = calculateRemovals(types);

                    // Pass the results back to the main thread
                    EntityChunkInspectionResult result = new EntityChunkInspectionResult(entitiesToRemove);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            applyChanges(result); // Apply changes on the main thread
                        }
                    }.runTask(plugin);
                } finally {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            handleChunkPostProcessing(chunkId);
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private @NotNull List<Entity> calculateRemovals(@NotNull Map<String, ArrayList<Entity>> types) {
        final ArrayList<Entity> entitiesToRemove = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Entity>> entry : types.entrySet()) {
            String entityType = entry.getKey();
            List<Entity> entityList = entry.getValue();
            int limit = config.getEntityLimit(entityType);

            if (entityList.size() > limit) {
                for (int i = entityList.size() - 1; i >= limit; i--) {
                    Entity entity = entityList.get(i);
                    if (!shouldPreserve(entity)) {
                        entitiesToRemove.add(entity);
                    }
                }
            }
        }
        return entitiesToRemove;
    }

    private void handleChunkPostProcessing(ChunkIdentifier chunkId) {
        AtomicBoolean flag = activeChunks.get(chunkId);
        if (flag == null) return;

        if (flag.get()) {
            // Reset flag and re-check
            flag.set(false);
            recheckChunk(chunkId);
        } else {
            // No pending changes; remove
            activeChunks.remove(chunkId);
        }
    }

    private void recheckChunk(@NotNull ChunkIdentifier chunkId) {
        final World world = Bukkit.getWorld(chunkId.getWorldUuid());
        if (world == null || !world.isChunkLoaded(chunkId.getX(), chunkId.getZ())) {
            activeChunks.remove(chunkId);
            return;
        }

        final Chunk chunk = world.getChunkAt(chunkId.getX(), chunkId.getZ());
        checkChunk(chunk);
    }

    public void applyChanges(final @NotNull EntityChunkInspectionResult result) {
        for (Entity entity: result.getEntitiesToRemove()) {
            if (entity instanceof ArmorStand) {
                handleArmorStand(entity); // Handle Armor Stands
                continue;
            }

            removeOrKillEntity(entity);
        }
    }

    private void handleArmorStand(Entity entity) {
        if (!(entity instanceof ArmorStand)) {
            return;
        }

        if (config.isDropItemsFromArmorStands()) {
            EntityEquipment entityEquipment = ((ArmorStand) entity).getEquipment();
            if (entityEquipment != null) {
                for (ItemStack itemStack : entityEquipment.getArmorContents()) {
                    entity.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
                }
            }
        }

        if (Util.isArmorStandTickDisabled()) {
            ChatUtil.logArmorStandTickWarning();
            entity.remove();
        }
    }

    private void removeOrKillEntity(Entity entity) {
        if (!config.isKillInsteadOfRemove() || !isKillable(entity)) {
            entity.remove();
            return;
        }

        killEntity(entity);
    }

    private void killEntity(final Entity entity) {
        ((Damageable) entity).setHealth(0.0D);
    }


    public static boolean isKillable(final Entity entity) {
        return entity instanceof Damageable;
    }


    private @NotNull Map<String, ArrayList<Entity>> addEntitiesByConfig(Entity @NotNull [] entities) {
        HashMap<String, ArrayList<Entity>> modifiedTypes = new HashMap<>();
        for (int i = entities.length - 1; i >= 0; i--) {
            final Entity entity = entities[i];

            String entityType = entity.getType().name();
            String entityMobGroup = MobGroupCompare.getMobGroup(entity);

            addEntityIfHasLimit(modifiedTypes, entityType, entity);
            addEntityIfHasLimit(modifiedTypes, entityMobGroup, entity);
        }
        return modifiedTypes;
    }

    private void addEntityIfHasLimit(Map<String, ArrayList<Entity>> modifiedTypes, String key, Entity entity) {
        if (config.hasEntityLimit(key)) {
            modifiedTypes.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
        }
    }

    private boolean shouldPreserve(Entity entity) {
        return hasMetaData(entity) || hasCustomName(entity) || entity instanceof Player || isPartOfRaid(entity);
    }

    private boolean hasCustomName(Entity entity) {
        return config.isPreserveNamedEntities() && entity.getCustomName() != null;
    }

    private boolean hasMetaData(Entity entity) {
        for (String metadata : config.getIgnoreMetadata()) {
            if (entity.hasMetadata(metadata)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPartOfRaid(Entity entity) {
        if (!config.isPreserveRaidEntities()) {
            return false;
        }

        if (entity instanceof Raider) {
            Raider raider = (Raider) entity;
            for (Raid raid : raider.getWorld().getRaids()) {
                boolean potentialMatch = raid.getRaiders().stream().anyMatch(r -> r.equals(raider));
                if (potentialMatch) {
                    return true;
                }
            }
        }
        return false;
    }
}
