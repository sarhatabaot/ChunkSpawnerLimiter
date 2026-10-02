package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.Set;

public class ExternalChecks {
    private static boolean hasNbtApi = false;
    private static Set<String> ignoredNbtKeys = Collections.emptySet();

    public static void setup(PluginConfig pluginConfig) {
        ignoredNbtKeys = Set.copyOf(pluginConfig.getIgnoreNbt());
        hasNbtApi = Bukkit.getPluginManager().getPlugin("NBT-API") != null;
    }

    public static boolean hasNbtData(final Entity entity) {
        if (!hasNbtApi || ignoredNbtKeys.isEmpty()) {
            return false;
        }

        return NBT.get(entity, nbt -> {
            for (String ignoredKey : ignoredNbtKeys) {
                if (nbt.hasTag(ignoredKey)) {
                    return true;
                }
            }
            return false;
        });
    }
}
