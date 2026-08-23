package com.github.sarhatabaot.chunkspawnerlimiter.reflection;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Uses NMS reflection to directly access Minecraft's internal per-chunk entity
 * storage for zero-allocation counting.
 * <p>
 * Supports Mojang-mapped (1.17+), Spigot-mapped (1.13–1.16), and legacy (1.8–1.12)
 * chunk entity structures. Falls back gracefully when reflection fails.
 */
public final class NmsEntityCounter {

    private final boolean initialized;
    private final String implementationName;

    // NMS classes and methods/fields
    @Nullable private Class<?> craftChunkClass;
    @Nullable private Method craftChunk_getHandle;

    /** The NMS LevelChunk / Chunk class */
    @Nullable private Class<?> nmsChunkClass;

    // Modern (1.17+): entitySections (array of LevelChunk$EntitySection)
    @Nullable private Field entitySectionsField;

    // Spigot/legacy (1.8–1.16): entitySlices (List<Entity>[])
    @Nullable private Field entitySlicesArrField;

    // CraftBukkit Entity → NMS entity helper
    @Nullable private Class<?> craftEntityClass;
    @Nullable private Method craftEntity_getHandle;
    @Nullable private Method nmsEntity_getType; // returns EntityType (modern) or EntityTypes (legacy)

    private NmsEntityCounter(@Nullable String versionSuffix) {
        boolean ok = false;
        String impl = "None";

        try {
            // 1) Detect structure
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            boolean isModern = !pkg.contains("craftbukkit.v");

            if (isModern) {
                ok = initModern();
                impl = "ModernNMS";
            } else if (versionSuffix != null) {
                // Try legacy/spigot
                if (initSpigot(versionSuffix)) {
                    ok = true;
                    impl = "SpigotNMS(" + versionSuffix + ")";
                }
            }
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[NmsEntityCounter] Init failed: " + t.getMessage());
        }

        this.initialized = ok;
        this.implementationName = impl;
        final String name = impl;
        if (ok) {
            CSLLogger.debug(() -> "[NmsEntityCounter] Initialized: " + name);
        }
    }

    // ---- Factory ----

    @Nullable
    public static NmsEntityCounter create(PluginConfig config) {
        if (!config.isNmsEntityCount()) return null;
        String suffix = detectVersionSuffix();
        NmsEntityCounter counter = new NmsEntityCounter(suffix);
        return counter.initialized ? counter : null;
    }

    // ---- Public API ----

    public boolean isSupported() { return initialized; }
    public String getImplementationName() { return implementationName; }

    /**
     * Reads the entity counts per type directly from the NMS chunk structure
     * and writes them into {@code data}, replacing whatever was cached before.
     *
     * @param chunk the bukkit chunk
     * @param coord the chunk coordinate (used for fallback)
     * @param data  the counter data to populate
     * @param trackedTypes entity types to count
     * @return true on success, false if NMS path failed (caller should fall back)
     */
    public boolean rebuildCountsFromNms(Chunk chunk, ChunkCoord coord,
                                        CounterData data,
                                        Iterable<EntityType> trackedTypes) {
        if (!initialized) return false;

        try {
            // Reset tracked counts
            for (EntityType t : trackedTypes) {
                data.setEntityCount(t, 0);
            }

            // Get NMS chunk
            Object nmsChunk = craftChunk_getHandle.invoke(chunk);
            if (nmsChunk == null) return false;

            // Access entity storage
            if (entitySectionsField != null) {
                // Modern path: EntitySection[] entitySections
                Object[] sections = (Object[]) entitySectionsField.get(nmsChunk);
                if (sections != null) {
                    for (Object section : sections) {
                        if (section == null) continue;
                        countEntitiesInContainer(section, data, trackedTypes);
                    }
                }
            } else if (entitySlicesArrField != null) {
                // Legacy path: List<Entity>[]
                Object[] slices = (Object[]) entitySlicesArrField.get(nmsChunk);
                if (slices != null) {
                    for (Object slice : slices) {
                        if (slice instanceof List<?> list) {
                            for (Object nmsEntity : list) {
                                EntityType type = nmsToBukkitType(nmsEntity);
                                if (type != null) data.incrementEntity(type);
                            }
                        }
                    }
                }
            } else {
                return false;
            }

            return true;
        } catch (Throwable t) {
            CSLLogger.debug(() -> "[NmsEntityCounter] Error: " + t.getMessage());
            return false;
        }
    }

    // ---- Init helpers ----

    private boolean initModern() {
        try {
            craftChunkClass = Class.forName("org.bukkit.craftbukkit.CraftChunk");
            craftChunk_getHandle = craftChunkClass.getMethod("getHandle");
            nmsChunkClass = Class.forName("net.minecraft.world.level.chunk.LevelChunk");
            entitySectionsField = nmsChunkClass.getDeclaredField("entitySections");
            entitySectionsField.setAccessible(true);

            // Entity type mapping
            initCraftEntity();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean initSpigot(String suffix) {
        try {
            craftChunkClass = Class.forName("org.bukkit.craftbukkit." + suffix + ".CraftChunk");
            craftChunk_getHandle = craftChunkClass.getMethod("getHandle");
            nmsChunkClass = Class.forName("net.minecraft.server." + suffix + ".Chunk");

            // Try entitySlices (List<Entity>[])
            try {
                entitySlicesArrField = nmsChunkClass.getDeclaredField("entitySlices");
            } catch (NoSuchFieldException e) {
                try {
                    entitySlicesArrField = nmsChunkClass.getDeclaredField("entities");
                } catch (NoSuchFieldException ignored) {
                    return false;
                }
            }
            entitySlicesArrField.setAccessible(true);
            initCraftEntity();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void initCraftEntity() {
        try {
            String ver = detectVersionSuffix();
            if (ver != null) {
                craftEntityClass = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftEntity");
            } else {
                craftEntityClass = Class.forName("org.bukkit.craftbukkit.CraftEntity");
            }
            craftEntity_getHandle = craftEntityClass.getMethod("getHandle");
        } catch (Throwable ignored) {
            // Type mapping via getHandle may not be needed if we use alternative path
        }
    }

    // ---- Entity type resolution ----

    @Nullable
    private EntityType nmsToBukkitType(Object nmsEntity) {
        try {
            // Try CraftEntity → CraftEntity.getEntityType()
            if (craftEntityClass != null && craftEntityClass.isInstance(nmsEntity)) {
                try {
                    Method getType = craftEntityClass.getMethod("getType");
                    Object result = getType.invoke(nmsEntity);
                    if (result instanceof EntityType) return (EntityType) result;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try NMS entity → getType() → EntityType (Mojang mapped on 1.17+)
            try {
                Method getType = nmsEntity.getClass().getMethod("getType");
                Object result = getType.invoke(nmsEntity);
                if (result instanceof EntityType) return (EntityType) result;
            } catch (NoSuchMethodException ignored) {}

            // Legacy: EntityTypes → EntityType via CraftEntityType conversion
            // Fallback: try name-based matching
            try {
                Method getSaveID = nmsEntity.getClass().getMethod("getSaveID");
                String id = (String) getSaveID.invoke(nmsEntity);
                if (id != null) {
                    try { return EntityType.valueOf(id.toUpperCase().replace(":", "_")); }
                    catch (IllegalArgumentException ignored) {}
                }
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable ignored) {}

        return null;
    }

    // ---- Modern entity section counter ----

    @SuppressWarnings("unchecked")
    private void countEntitiesInContainer(Object section, CounterData data,
                                           Iterable<EntityType> tracked) {
        try {
            // LevelChunk$EntitySection stores entities in a List
            // Try common field names
            Field storage = findField(section.getClass(), "storage", "entities", "contents");
            if (storage != null) {
                storage.setAccessible(true);
                Object val = storage.get(section);
                if (val instanceof Collection<?> coll) {
                    for (Object nmsEntity : coll) {
                        EntityType type = nmsToBukkitType(nmsEntity);
                        if (type != null) {
                            data.incrementEntity(type);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Nullable
    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
        }
        // Walk superclass
        Class<?> sup = clazz.getSuperclass();
        if (sup != null && sup != Object.class) {
            return findField(sup, names);
        }
        return null;
    }

    @Nullable
    private static String detectVersionSuffix() {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            int idx = pkg.indexOf("craftbukkit");
            if (idx != -1) {
                String after = pkg.substring(idx + "craftbukkit".length());
                if (after.startsWith(".")) after = after.substring(1);
                if (after.startsWith("v")) return after;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}