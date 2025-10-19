package com.github.sarhatabaot.chunkspawnerlimiter.reflection;

import java.lang.reflect.Method;
import java.util.Collection;

public final class RaidReflection {

    private static final boolean SUPPORTED;
    private static final Class<?> RAIDER_CLASS;
    private static final Class<?> RAID_CLASS;
    private static final Method GET_WORLD;
    private static final Method GET_RAIDS;
    private static final Method GET_RAIDERS;

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
        RAID_CLASS = raid;
        GET_WORLD = getWorld;
        GET_RAIDS = getRaids;
        GET_RAIDERS = getRaiders;
        SUPPORTED = supported;
    }

    private RaidReflection() {}

    /**
     * @return true if this server version supports raids.
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Checks if a given entity is part of an active raid.
     */
    public static boolean isEntityInRaid(Object entity) {
        if (!SUPPORTED || entity == null || !RAIDER_CLASS.isInstance(entity)) {
            return false;
        }

        try {
            Object world = GET_WORLD.invoke(entity);
            @SuppressWarnings("unchecked")
            Collection<?> raids = (Collection<?>) GET_RAIDS.invoke(world);

            for (Object raid : raids) {
                @SuppressWarnings("unchecked")
                Collection<?> raiders = (Collection<?>) GET_RAIDERS.invoke(raid);
                if (raiders.contains(entity)) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        return false;
    }
}

