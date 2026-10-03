package com.github.sarhatabaot.chunkspawnerlimiter.reflection;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public final class RaidReflection {

    private static final Class<?> RAIDER_CLASS;
    private static final Method GET_RAIDS;
    private static final Method GET_RAIDERS;
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();
    private static volatile boolean available;

    static {
        Class<?> raider = null;
        Class<?> raid = null;
        Method getWorld = null;
        Method getRaids = null;
        Method getRaiders = null;
        boolean supported;

        try {
            // Try to load classes (only available in 1.14+)
            raider = Class.forName("org.bukkit.entity.Raider");
            raid = Class.forName("org.bukkit.Raid");

            // Cache methods
            getWorld = raider.getMethod("getWorld");
            getRaids = raider.getMethod("getWorld").getReturnType().getMethod("getRaids");
            getRaiders = raid.getMethod("getRaiders");

            supported = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Older server version — feature not available
            supported = false;
        }

        RAIDER_CLASS = raider;
        GET_RAIDS = getRaids;
        GET_RAIDERS = getRaiders;
        available = supported;
    }

    private RaidReflection() {}

    /**
     * @return true if this server version supports raids.
     */
    public static boolean isSupported() {
        return available;
    }

    /**
     * Checks if a given entity is part of an active raid.
     */
    public static boolean isEntityInRaid(Object entity) {
        if (!available || !RAIDER_CLASS.isInstance(entity)) {
            return false;
        }

        return entity instanceof Entity bukkitEntity
                && getActiveRaiderUuids(bukkitEntity.getWorld()).contains(bukkitEntity.getUniqueId());
    }

    public static Set<UUID> getActiveRaiderUuids(World world) {
        if (!available) {
            return Collections.emptySet();
        }

        try {
            Collection<?> raids = (Collection<?>) GET_RAIDS.invoke(world);
            Set<UUID> raiderUuids = new HashSet<>();

            for (Object raid : raids) {
                Collection<?> raiders = (Collection<?>) GET_RAIDERS.invoke(raid);
                for (Object raider : raiders) {
                    if (raider instanceof Entity entity) {
                        raiderUuids.add(entity.getUniqueId());
                    }
                }
            }
            return raiderUuids;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            available = false;
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                Bukkit.getLogger().log(Level.WARNING,
                        "[RaidReflection] Unable to inspect active raids; disabling raid reflection: "
                                + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        return Collections.emptySet();
    }
}

