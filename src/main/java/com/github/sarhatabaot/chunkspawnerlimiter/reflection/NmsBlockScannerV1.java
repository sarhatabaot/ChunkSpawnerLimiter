package com.github.sarhatabaot.chunkspawnerlimiter.reflection;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;

// with latest minecraft un-obfuscating nms, this could become easier with newer versions.
public class NmsBlockScannerV1 {
    private static final int CHUNK_SIZE = 16;

    private final PluginConfig pluginConfig;
    private final CounterDataManager counterDataManager;

    private String version = null;
    private final boolean legacy;
    private final Method getTypeMethod;
    private final Method getHandleMethod;
    private final Constructor<?> blockPosConstructor;
    private final Method getBlockMethod;
    private final Method craftMagicNumbersGetMaterial;

    public NmsBlockScannerV1(
            final PluginConfig pluginConfig,
            final CounterDataManager counterDataManager
    ) {
        this.pluginConfig = pluginConfig;
        this.counterDataManager = counterDataManager;

        try {
            this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            this.legacy = isLegacyVersion(version);

            // Reflection setup
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            Class<?> nmsWorldClass = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> blockPosClass = Class.forName("net.minecraft.server." + version + ".BlockPosition");

            this.getHandleMethod = craftWorldClass.getMethod("getHandle");
            this.blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            this.getTypeMethod = nmsWorldClass.getMethod("getType", blockPosClass);

            // IBlockData class and conversion methods
            Class<?> iBlockDataClass = Class.forName("net.minecraft.server." + version + ".IBlockData");

            if (legacy) {
                // 1.8 - 1.12
                this.getBlockMethod = iBlockDataClass.getMethod("getBlock");
                this.craftMagicNumbersGetMaterial = Class
                        .forName("org.bukkit.craftbukkit." + version + ".util.CraftMagicNumbers")
                        .getMethod("getMaterial", Class.forName("net.minecraft.server." + version + ".Block"));
            } else {
                // 1.13+
                Method method = null;
                try {
                    method = iBlockDataClass.getMethod("getBukkitMaterial");
                } catch (NoSuchMethodException ignored) {
                    // fallback later
                }
                this.getBlockMethod = method;
                this.craftMagicNumbersGetMaterial = null;
            }

            Bukkit.getLogger().log(Level.INFO, "[CSL] NMS Block Scanner initialized for {0} (legacy={1})",
                    new Object[]{version, legacy});

        } catch (Exception e) {
            StringBuilder detail = new StringBuilder();
            detail.append("\n\n=== NMS BLOCK SCANNER INITIALIZATION FAILED ===\n");

            detail.append("Bukkit version: ").append(Bukkit.getVersion()).append("\n");
            detail.append("Detected server class: ").append(Bukkit.getServer().getClass().getName()).append("\n");
            detail.append("Detected server package: ")
                    .append(Bukkit.getServer().getClass().getPackage().getName()).append("\n");
            detail.append("Extracted NMS version: ").append(this.version).append("\n\n");

            detail.append("Attempted to load classes:\n");
            detail.append("  CraftWorld:        org.bukkit.craftbukkit.")
                    .append(version).append(".CraftWorld\n");
            detail.append("  NMS World:         net.minecraft.server.")
                    .append(version).append(".World\n");
            detail.append("  BlockPosition:     net.minecraft.server.")
                    .append(version).append(".BlockPosition\n");
            detail.append("  IBlockData:        net.minecraft.server.")
                    .append(version).append(".IBlockData\n");

            if (isLegacyVersion(version)) {
                detail.append("\nLegacy mode (<= 1.12):\n");
                detail.append("  getBlock():        IBlockData#getBlock\n");
                detail.append("  CraftMagicNumbers: org.bukkit.craftbukkit.")
                        .append(version).append(".util.CraftMagicNumbers#getMaterial\n");
            } else {
                detail.append("\nModern mode (1.13+):\n");
                detail.append("  getBukkitMaterial: IBlockData#getBukkitMaterial (if exists)\n");
            }

            detail.append("\nRoot cause: ").append(e.getClass().getName())
                    .append(": ").append(e.getMessage()).append("\n");
            detail.append("===============================================\n\n");

            throw new RuntimeException(
                    "Failed to initialize NmsBlockScanner for Bukkit version " + Bukkit.getVersion()
                            + ", extracted NMS package: " + this.version
                            + ".\n\nDetailed info:\n" + detail,
                    e
            );
//            throw new RuntimeException("Failed to initialize NmsBlockScanner for " + Bukkit.getVersion() + ", version: " + this.version, e);
        }
    }

    /**
     * Returns the Material at a given world coordinate using NMS reflection.
     */
    public Material getBlockType(
            final World world,
            final int x,
            final int y,
            final int z
    ) {
        try {
            Object nmsWorld = getHandleMethod.invoke(world);
            Object blockPos = blockPosConstructor.newInstance(x, y, z);
            Object iBlockData = getTypeMethod.invoke(nmsWorld, blockPos);

            if (legacy) {
                Object nmsBlock = getBlockMethod.invoke(iBlockData);
                return (Material) craftMagicNumbersGetMaterial.invoke(null, nmsBlock);
            } else if (getBlockMethod != null) {
                Object material = getBlockMethod.invoke(iBlockData);
                if (material instanceof Material) {
                    return (Material) material;
                }
            }

            // Fallback for future versions
            return world.getBlockAt(x, y, z).getType();

        } catch (Throwable t) {
            // Safety fallback to AIR to prevent crashing
            return Material.AIR;
        }
    }

    public void scanChunk(final @NotNull Chunk chunk, final ChunkCoord chunkCoord) {
        World world = chunk.getWorld();

        int startX = chunk.getX() << 4;
        int startZ = chunk.getZ() << 4;

        int minY = WorldReflection.getWorldMinHeightSafe(world);
        int maxY = world.getMaxHeight();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = minY; y < maxY; y++) {
                    Material type = getBlockType(world, startX + x, y, startZ + z);
                    if (pluginConfig.hasBlockLimit(type.name())) {
                        counterDataManager.getCounterData(chunkCoord).incrementBlock(type);
                    }
                }
            }
        }
    }

    private boolean isLegacyVersion(@NotNull String version) {
        // Legacy means pre-flattening (1.8–1.12)
        return version.startsWith("v1_8")
                || version.startsWith("v1_9")
                || version.startsWith("v1_10")
                || version.startsWith("v1_11")
                || version.startsWith("v1_12");
    }
}
