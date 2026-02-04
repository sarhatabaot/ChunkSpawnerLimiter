package com.github.sarhatabaot.chunkspawnerlimiter.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for handling spawn egg operations, specifically for returning eggs to players
 * when entity spawn events are cancelled by removal modes.
 * 
 * This class is designed to work with Minecraft 1.8.8 and uses the legacy MONSTER_EGG system.
 */
public class SpawnEggUtil {
    
    /**
     * Checks if a spawn reason indicates the entity was spawned from a spawn egg.
     * 
     * @param spawnReason the spawn reason to check
     * @return true if the spawn reason indicates a spawn egg was used
     */
    public static boolean isSpawnEggSpawn(String spawnReason) {
        if (spawnReason == null) {
            return false;
        }
        
        // Convert to uppercase for comparison
        String reason = spawnReason.toUpperCase();
        
        // Check for various spawn egg related reasons
        return reason.contains("EGG") || 
               reason.equals("SPAWNER") ||
               reason.equals("SPAWNER_EGG") ||
               reason.equals("DISPENSER"); // Dispenser can also spawn eggs
    }
    
    /**
     * Gets the corresponding spawn egg material for a given entity type.
     * For Minecraft 1.8.8 compatibility, this uses the legacy MONSTER_EGG system.
     * 
     * @param entityType the entity type to get the spawn egg for
     * @return the Material for the spawn egg, or null if not found
     */
    public static Material getSpawnEggMaterial(EntityType entityType) {
        if (entityType == null) {
            return null;
        }
        
        try {
            // For 1.8.8 compatibility, we use the legacy monster egg system
            // Most entities use MONSTER_EGG with metadata to determine the entity type
            return Material.MONSTER_EGG;
        } catch (IllegalArgumentException e) {
            // If MONSTER_EGG doesn't exist, try the modern system
            try {
                String materialName = entityType.name() + "_SPAWN_EGG";
                return Material.valueOf(materialName);
            } catch (IllegalArgumentException e2) {
                // If neither works, return null
                return null;
            }
        }
    }
    
    /**
     * Creates an ItemStack representing a spawn egg for the given entity type.
     * 
     * @param entityType the entity type
     * @return an ItemStack for the spawn egg, or null if not supported
     */
    public static ItemStack createSpawnEggItem(EntityType entityType) {
        Material eggMaterial = getSpawnEggMaterial(entityType);
        if (eggMaterial == null) {
            return null;
        }
        
        return new ItemStack(eggMaterial, 1);
    }
    
    /**
     * Drops a spawn egg at the specified location for the given entity type.
     * 
     * @param entityType the entity type that was spawned
     * @param location the location where the egg should be dropped
     * @return true if the egg was successfully dropped, false otherwise
     */
    public static boolean dropSpawnEgg(EntityType entityType, Location location) {
        if (entityType == null || location == null) {
            return false;
        }
        
        ItemStack eggItem = createSpawnEggItem(entityType);
        if (eggItem == null) {
            return false;
        }
        
        // Drop the item at the location
        location.getWorld().dropItemNaturally(location, eggItem);
        return true;
    }
}
