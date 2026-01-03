package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.RaidReflection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class Checks {
    private static PluginConfig pluginConfig;

    public static void setup(PluginConfig pluginConfig) {
        Checks.pluginConfig = pluginConfig;
    }

    //is player & kill players is disabled
    public static boolean shouldSkipPlayers(final Entity entity) {
        return entity instanceof Player && !pluginConfig.isKillPlayers();
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

        return RaidReflection.isEntityInRaid(entity);
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
