package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.WorldReflection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Level;


/**
 * NmsBlockScanner
 * <p>
 * Provides utility to get block Material at coordinates using NMS when possible.
 * Safe fallbacks to Bukkit API included.
 */
public class NmsBlockScanner {
    private static final int CHUNK_SIZE = 16;
    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;

    // Reflection-found classes & methods (nullable if not found)
    private Class<?> craftWorldClass;
    private Method craftWorld_getHandle;

    private Class<?> nmsWorldClass; // e.g. net.minecraft.server.level.WorldServer or net.minecraft.world.level.World or legacy
    private Method nmsWorld_getBlockStateMethod; // candidate names: getBlockState, getType, getType

    private Class<?> blockPosClass;
    private Constructor<?> blockPosConstructor; // (int x, int y, int z) or (double x,..) depending on version

    private Class<?> iBlockDataClass; // net.minecraft.world.level.block.state.BlockState / IBlockData
    private Method iBlockData_getBukkitMaterial; // Paper convenience method (if present)
    private Method iBlockData_getBlock; // if IBlockData#getBlock() exists (legacy -> returns Block)

    // Optional CraftBukkit helper (fallback if plugin runs on CraftBukkit)
    private Method craftMagicNumbers_getMaterial; // CraftMagicNumbers.getMaterial(Block) (fallback)

    // Whether we successfully found at least basic NMS wiring
    private boolean initialized = false;

    public NmsBlockScanner(final PluginConfig pluginConfig, final CounterDataManager counterDataManager) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;
        try {
            init();
            initialized = true;
            Bukkit.getLogger().log(Level.INFO, "[NmsBlockScanner] Initialized (mojang-first).");
        } catch (Throwable t) {
            // Do not fatal — keep initialized=false and provide safe fallbacks
            initialized = false;
            Bukkit.getLogger().log(Level.WARNING, "[NmsBlockScanner] Initialization partially failed. Falling back to Bukkit API. "
                    + "Detailed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            logDetailedInitError(t);
        }
    }

    private void init() throws Exception {
        // 1) find CraftWorld class and getHandle()
        try {
            craftWorldClass = tryLoad(
                    "org.bukkit.craftbukkit.CraftWorld", // some shading might remove version segment
                    // ideally full name with version is not needed; Class.forName will succeed if present on classpath
                    "org.bukkit.craftbukkit." + detectCraftBukkitPackageSuffix() + ".CraftWorld",
                    "org.bukkit.craftbukkit.v1_0_R_NOTFOUND.CraftWorld" // dummy, ignored
            );
        } catch (ClassNotFoundException e) {
            // craftWorldClass may still be available via Bukkit world object's class' package
            craftWorldClass = null;
        }

        // If we couldn't load CraftWorld directly, still try to get handle method from any world instance's class
        if (craftWorldClass != null) {
            try {
                craftWorld_getHandle = craftWorldClass.getMethod("getHandle");
            } catch (NoSuchMethodException ignored) {
                craftWorld_getHandle = null;
            }
        }

        // 2) nms World candidates (modern-first)
        String[] nmsWorldCandidates = new String[]{
                "net.minecraft.server.level.WorldServer",   // modern server world (1.17+)
                "net.minecraft.world.level.World",          // alternative modern path
                // legacy server world pattern (pre-1.17). We'll attempt to extract package suffix if possible:
                "net.minecraft.server." + detectCraftBukkitPackageSuffix() + ".World"
        };

        nmsWorldClass = tryLoadNullable(nmsWorldCandidates);

        // 3) BlockPos candidates
        String[] blockPosCandidates = new String[]{
                "net.minecraft.core.BlockPos",                       // modern (1.17+)
                "net.minecraft.world.phys.BlockPos",                // some variants, try conservative
                "net.minecraft.server." + detectCraftBukkitPackageSuffix() + ".BlockPosition"
        };

        blockPosClass = tryLoadNullable(blockPosCandidates);

        if (blockPosClass != null) {
            // prefer constructor (int,int,int)
            try {
                blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            } catch (NoSuchMethodException ex) {
                // try long or three ints as alternative signatures sometimes exist; try (long)
                try {
                    blockPosConstructor = blockPosClass.getConstructor(long.class);
                } catch (NoSuchMethodException ex2) {
                    blockPosConstructor = null;
                }
            }
        }

        // 4) IBlockData / BlockState class candidates
        String[] iBlockCandidates = new String[]{
                "net.minecraft.world.level.block.state.BlockState", // possible modern name
                "net.minecraft.world.level.block.state.IBlockData", // alternate
                "net.minecraft.server." + detectCraftBukkitPackageSuffix() + ".IBlockData" // legacy
        };

        iBlockDataClass = tryLoadNullable(iBlockCandidates);

        if (iBlockDataClass != null) {
            // try to find a Paper convenience method getBukkitMaterial()
            try {
                iBlockData_getBukkitMaterial = iBlockDataClass.getMethod("getBukkitMaterial");
            } catch (NoSuchMethodException ignored) {
                iBlockData_getBukkitMaterial = null;
            }

            // try legacy getBlock()
            try {
                iBlockData_getBlock = iBlockDataClass.getMethod("getBlock");
            } catch (NoSuchMethodException ignored) {
                iBlockData_getBlock = null;
            }
        }

        // 5) find a method on nmsWorldClass to fetch block state
        if (nmsWorldClass != null && blockPosClass != null) {
            // candidate method names
            String[] methodNames = new String[]{"getType", "getBlockState", "a", "getTypeAndData", "b"}; // 'a'/'b' are obf fallbacks
            for (String name : methodNames) {
                try {
                    nmsWorld_getBlockStateMethod = nmsWorldClass.getMethod(name, blockPosClass);
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            // if still null try methods with primitive coordinates (x,y,z)
            if (nmsWorld_getBlockStateMethod == null) {
                try {
                    nmsWorld_getBlockStateMethod = nmsWorldClass.getMethod("getType", int.class, int.class, int.class);
                } catch (NoSuchMethodException ignored) {
                    nmsWorld_getBlockStateMethod = null;
                }
            }
        }

        // 6) Attempt to locate CraftMagicNumbers.getMaterial(Block) as a last resort for conversion
        try {
            Class<?> craftMagicNumbers = tryLoad("org.bukkit.craftbukkit.util.CraftMagicNumbers",
                    "org.bukkit.craftbukkit." + detectCraftBukkitPackageSuffix() + ".util.CraftMagicNumbers");
            craftMagicNumbers_getMaterial = craftMagicNumbers.getMethod("getMaterial", tryLoad(
                    "net.minecraft.server." + detectCraftBukkitPackageSuffix() + ".Block",
                    "net.minecraft.world.level.block.Block" // modern
            ));
        } catch (Throwable ignored) {
            craftMagicNumbers_getMaterial = null;
        }

        // log what we've found
        String summary = String.format("NMS init summary: craftWorld=%s, nmsWorld=%s, blockPos=%s, iBlockData=%s, worldGetMethod=%s",
                safeName(craftWorldClass), safeName(nmsWorldClass), safeName(blockPosClass), safeName(iBlockDataClass),
                nmsWorld_getBlockStateMethod == null ? "null" : nmsWorld_getBlockStateMethod.getName());
        Bukkit.getLogger().log(Level.INFO, "[NmsBlockScanner] " + summary);
    }

    /**
     * Primary API: get the Bukkit Material at x,y,z in the provided Bukkit World.
     * Tries NMS path first; if anything fails, falls back to Bukkit API (World#getBlockAt).
     */
    public Material getMaterialAt(World bukkitWorld, int x, int y, int z) {
        // If initialization partially failed, just fall back
        if (!initialized && craftWorld_getHandle == null && nmsWorld_getBlockStateMethod == null) {
            return fallbackBukkit(bukkitWorld, x, y, z);
        }

        try {
            Object nmsWorld = resolveNmsWorldFromBukkitWorld(bukkitWorld);
            if (nmsWorld == null || nmsWorld_getBlockStateMethod == null) {
                return fallbackBukkit(bukkitWorld, x, y, z);
            }

            Object blockPos = createBlockPos(x, y, z);
            if (blockPos == null) {
                return fallbackBukkit(bukkitWorld, x, y, z);
            }

            Object iBlock = invokeGetBlockState(nmsWorld, blockPos, x, y, z);
            if (iBlock == null) {
                return fallbackBukkit(bukkitWorld, x, y, z);
            }

            // 1) try Paper / modern convenience
            if (iBlockData_getBukkitMaterial != null) {
                try {
                    Object matObj = iBlockData_getBukkitMaterial.invoke(iBlock);
                    if (matObj instanceof Material) return (Material) matObj;
                    if (matObj != null && matObj.toString() != null) {
                        try {
                            return Material.valueOf(matObj.toString());
                        } catch (IllegalArgumentException ignored) {}
                    }
                } catch (Throwable ignore) { /* continue to fallback */ }
            }

            // 2) try IBlockData#getBlock() -> CraftMagicNumbers.getMaterial(block)
            if (iBlockData_getBlock != null && craftMagicNumbers_getMaterial != null) {
                try {
                    Object nmsBlock = iBlockData_getBlock.invoke(iBlock);
                    Object mat = craftMagicNumbers_getMaterial.invoke(null, nmsBlock);
                    if (mat instanceof Material) return (Material) mat;
                } catch (Throwable ignore) { /* continue to fallback */ }
            }

            // 3) As an additional path: if iBlock.toString contains block id name try mapping (not reliable)
            try {
                String s = iBlock.toString();
                if (s != null) {
                    for (Material m : Material.values()) {
                        if (s.contains(m.name())) return m;
                    }
                }
            } catch (Throwable ignore) {}

            // final fallback: Bukkit API
            return fallbackBukkit(bukkitWorld, x, y, z);

        } catch (Throwable t) {
            // On any reflection/runtime error, log and return safe fallback
            Bukkit.getLogger().log(Level.WARNING, "[NmsBlockScanner] Failed to resolve material via NMS: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            logDetailedInitError(t);
            return fallbackBukkit(bukkitWorld, x, y, z);
        }
    }

    public void scanChunk(@NotNull Chunk chunk, @NotNull ChunkCoord chunkCoord, final int chunkSize) {
        final World world = chunk.getWorld();

        // Chunk → world coords
        final int startX = chunk.getX() << 4;
        final int startZ = chunk.getZ() << 4;

        // Safe min/max Y for all versions
        final int minY = WorldReflection.getWorldMinHeightSafe(world);
        final int maxY = world.getMaxHeight();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = minY; y < maxY; y++) {

                    Material type = getMaterialAt(world, startX + x, y, startZ + z);

                    if (type != null && pluginConfig.hasBlockLimit(type.name())) {
                        counterDataManager.getCounterData(chunkCoord).incrementBlock(type);
                    }
                }
            }
        }
    }


    public void scanChunk(@NotNull Chunk chunk, @NotNull ChunkCoord chunkCoord) {
        scanChunk(chunk, chunkCoord, CHUNK_SIZE);
    }

    // ----------------------
    // Reflection helpers
    // ----------------------

    private @Nullable Object invokeGetBlockState(Object nmsWorld, Object blockPos, int x, int y, int z) throws Exception {
        if (nmsWorld_getBlockStateMethod == null) return null;
        try {
            return nmsWorld_getBlockStateMethod.invoke(nmsWorld, blockPos);
        } catch (IllegalArgumentException iae) {
            // maybe method expects (int,int,int)
            try {
                return nmsWorld_getBlockStateMethod.invoke(nmsWorld, x, y, z);
            } catch (IllegalArgumentException iae2) {
                throw iae2;
            }
        }
    }

    private @Nullable Object createBlockPos(int x, int y, int z) {
        if (blockPosConstructor != null) {
            try {
                Class<?>[] params = blockPosConstructor.getParameterTypes();
                if (params.length == 3 && params[0] == int.class) {
                    return blockPosConstructor.newInstance(x, y, z);
                } else if (params.length == 1 && params[0] == long.class) {
                    long packed = ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF);
                    return blockPosConstructor.newInstance(packed);
                } else {
                    // last resort: attempt with ints via reflection boxing
                    Object[] args = new Object[]{x, y, z};
                    return blockPosConstructor.newInstance(args);
                }
            } catch (Throwable t) {
                // fall through
                return null;
            }
        }
        return null;
    }

    private @Nullable Object resolveNmsWorldFromBukkitWorld(World bukkitWorld) throws Exception {
        // If we have craftWorld_getHandle, prefer it
        if (craftWorld_getHandle != null && craftWorldClass != null && craftWorldClass.isAssignableFrom(bukkitWorld.getClass())) {
            // e.g. cast and call getHandle
            try {
                return craftWorld_getHandle.invoke(bukkitWorld);
            } catch (Throwable ignored) {
            }
        }

        // If CraftWorld not available or not assignable, try to find a method 'getHandle' on the runtime class of the bukkit world
        try {
            Method maybeGetHandle = bukkitWorld.getClass().getMethod("getHandle");
            if (maybeGetHandle != null) {
                return maybeGetHandle.invoke(bukkitWorld);
            }
        } catch (NoSuchMethodException ignored) {
        }

        // Last resort: try to call Bukkit.getServer() class / internal mapping — but this is fragile.
        return null;
    }

    private Material fallbackBukkit(World world, int x, int y, int z) {
        try {
            return world.getBlockAt(x, y, z).getType();
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "[NmsBlockScanner] Bukkit fallback failed: " + t.getMessage());
            return Material.BEDROCK; // safe default (shouldn't happen though)
        }
    }

    // ----------------------
    // Utility / loading helpers
    // ----------------------

    private String detectCraftBukkitPackageSuffix() {
        try {
            String serverClass = Bukkit.getServer().getClass().getPackage().getName(); // e.g. org.bukkit.craftbukkit.v1_20_R3 or io.papermc.paper
            // If it contains "craftbukkit.", return the suffix after craftbukkit.
            int idx = serverClass.indexOf("craftbukkit");
            if (idx != -1) {
                String after = serverClass.substring(idx + "craftbukkit".length());
                // after example: ".v1_20_R3"
                if (after.startsWith(".")) after = after.substring(1);
                if (after.isEmpty()) return ""; // unusual
                return after;
            }

            // otherwise try to extract version-like segment (fallback)
            String[] parts = serverClass.split("\\.");
            if (parts.length >= 4) {
                return parts[3];
            }
            return serverClass; // last resort
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private Class<?> tryLoad(String... names) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String n : names) {
            if (n == null || n.trim().isEmpty()) continue;
            try {
                return Class.forName(n);
            } catch (ClassNotFoundException ex) {
                last = ex;
            } catch (NoClassDefFoundError err) {
                last = new ClassNotFoundException(err.toString(), err);
            }
        }
        throw last == null ? new ClassNotFoundException(Arrays.toString(names)) : last;
    }

    private Class<?> tryLoadNullable(String... names) {
        for (String n : names) {
            if (n == null || n.trim().isEmpty()) continue;
            try {
                return Class.forName(n);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private String safeName(Class<?> c) {
        return c == null ? "null" : c.getName();
    }

    private void logDetailedInitError(Throwable t) {
        // Avoid spamming stacktrace to console in production; show concise info.
        Bukkit.getLogger().log(Level.INFO, "[NmsBlockScanner] Detailed failure: " + t.getClass().getName() + ": " + t.getMessage());
        // Optionally, to aid debugging, print the cause chain:
        Throwable cause = t;
        while (cause != null) {
            Bukkit.getLogger().log(Level.FINER, "Cause: " + cause.getClass().getName() + ": " + cause.getMessage());
            cause = cause.getCause();
        }
    }

}
