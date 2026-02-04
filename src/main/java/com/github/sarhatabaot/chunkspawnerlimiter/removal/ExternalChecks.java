package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class ExternalChecks {
    private static PluginConfig pluginConfig;
    private static boolean hasNbtApi = false;

    public static void setup(PluginConfig pluginConfig) {
        ExternalChecks.pluginConfig = pluginConfig;

        if (Bukkit.getPluginManager().getPlugin("NBT-API") != null) {
            ExternalChecks.hasNbtApi = true;
        }

    }

    public static boolean hasNbtData(final Entity entity) {
        if (!hasNbtApi || pluginConfig.getIgnoreNbt().isEmpty())
            return false;

        for (String ignore: pluginConfig.getIgnoreNbt()) {
            boolean hasNbt = NBT.get(entity, nbt -> {
                return nbt.hasTag(ignore);
            });
            if (hasNbt) {
                return true;
            }
        }
        return false;
    }
}
