package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.bukkit.entity.Entity;

import java.util.Collection;

public class Checks {
    private static PluginConfig pluginConfig;

    public static void setup(PluginConfig pluginConfig) {
        Checks.pluginConfig = pluginConfig;
    }

    public static boolean hasCustomName(final Entity entity) {
        if (!pluginConfig.shouldPreserveNamedEntities()) {
            return false;
        }
        return entity.getCustomName() != null;
    }


    public static boolean isPartOfRaid(Entity entity) {
        if (!pluginConfig.shouldPreserveRaidEntities()) {
            return false;
        }

        return isEntityInRaid(entity);
    }

    public static boolean isEntityInRaid(Object entity) {
        try {
            // Try to load the Raider and Raid classes (1.14+)
            Class<?> raiderClass = Class.forName("org.bukkit.entity.Raider");
            Class<?> raidClass = Class.forName("org.bukkit.Raid");

            // Check if this entity is actually a Raider
            if (!raiderClass.isInstance(entity)) {
                return false;
            }

            // Get the raider's world
            Object world = entity.getClass().getMethod("getWorld").invoke(entity);

            // getRaids() -> returns Collection<Raid>
            @SuppressWarnings("unchecked")
            Collection<?> raids = (Collection<?>) world.getClass().getMethod("getRaids").invoke(world);

            // For each raid: raid.getRaiders().stream().anyMatch(r -> r.equals(raider))
            for (Object raid : raids) {
                @SuppressWarnings("unchecked")
                Collection<?> raiders = (Collection<?>) raidClass.getMethod("getRaiders").invoke(raid);
                if (raiders.contains(entity)) {
                    return true;
                }
            }

        } catch (ClassNotFoundException e) {
            // Raider or Raid class not available in this version (1.13 or earlier)
            return false;
        } catch (ReflectiveOperationException e) {
            CSLLogger.debug(e.getMessage());
        }

        return false;
    }



    public static boolean hasMetaData(final Entity entity) {
        if (pluginConfig.getIgnoreMetadata().isEmpty()) {
            return false;
        }

        for (final String metadata: pluginConfig.getIgnoreMetadata()) {
            if (entity.hasMetadata(metadata))
                return true;
        }
        return false;
    }

    public static boolean isUnderOrEqualToLimit(int count, int limit) {
        return count + 1 <= limit;
    }
}
