package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.AbstractBlockScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.util.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * NMS block scanner for Minecraft 1.17+ using Mojang-mapped classes.
 * <p>
 * Includes chunk-section palette fast-path to skip sections that contain no
 * tracked blocks, and air-only sections.
 */
public class ModernNmsScanner extends AbstractBlockScanner {

    private final boolean initialized;

    // NMS classes (Mojang-mapped)
    private Class<?> blockPosClass;
    private Class<?> blockStateClass;

    // Methods
    private Method craftWorld_getHandle;
    private Method getBlockState;
    private Method getBukkitMaterial; // Paper optimization
    private Method getBlock;
    private Method craftMagicNumbers_getMaterial;

    // Constructor
    private Constructor<?> blockPosConstructor;

    // Section palette fast-path (1.17+)
    private Class<?> levelChunkClass;
    private Method chunk_getSection;
    private Method section_hasOnlyAir;
    private Object nmsChunkHandle; // cached during section scan

    public ModernNmsScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        super(plugin, config, counterManager);
        this.initialized = initialize();
    }

    private boolean initialize() {
        try {
            // CraftWorld.getHandle()
            String suffix = MinecraftVersion.detectVersionSuffix();
            Class<?> craftWorldClass;
            if (suffix != null) {
                craftWorldClass = Class.forName("org.bukkit.craftbukkit." + suffix + ".CraftWorld");
            } else {
                craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            }
            craftWorld_getHandle = craftWorldClass.getMethod("getHandle");

            // ServerLevel
            Class<?> serverLevelClass = loadClass(
                "net.minecraft.server.level.ServerLevel",
                "net.minecraft.server.level.WorldServer"
            );
            if (serverLevelClass == null) return false;

            // BlockPos / BlockState
            blockPosClass = loadClass("net.minecraft.core.BlockPos", "net.minecraft.core.BlockPosition");
            if (blockPosClass == null) return false;
            blockStateClass = loadClass("net.minecraft.world.level.block.state.BlockState",
                                        "net.minecraft.world.level.block.state.IBlockData");
            if (blockStateClass == null) return false;

            blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            getBlockState = findMethod(serverLevelClass, blockPosClass, "getBlockState", "getType");
            if (getBlockState == null) return false;

            try { getBukkitMaterial = blockStateClass.getMethod("getBukkitMaterial"); }
            catch (NoSuchMethodException ignored) {}
            try { getBlock = blockStateClass.getMethod("getBlock"); }
            catch (NoSuchMethodException ignored) {}

            // CraftMagicNumbers
            try {
                Class<?> cmn = loadClass("org.bukkit.craftbukkit.util.CraftMagicNumbers",
                    "org.bukkit.craftbukkit." + (suffix != null ? suffix + "." : "") + "util.CraftMagicNumbers");
                Class<?> blockCls = loadClass("net.minecraft.world.level.block.Block");
                if (cmn != null && blockCls != null)
                    craftMagicNumbers_getMaterial = cmn.getMethod("getMaterial", blockCls);
            } catch (NoSuchMethodException ignored) {}

            // Section palette
            initSectionPalette();

            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ModernNMS] Init failed: " + e.getMessage());
            return false;
        }
    }

    private void initSectionPalette() {
        try {
            levelChunkClass = loadClass("net.minecraft.world.level.chunk.LevelChunk",
                                        "net.minecraft.server.level.Chunk");
            if (levelChunkClass == null) return;
            chunk_getSection = findMethod(levelChunkClass, int.class, "getSection");
            if (chunk_getSection == null) return;

            Class<?> sectionClass = loadClass("net.minecraft.world.level.chunk.LevelChunkSection",
                                              "net.minecraft.world.level.chunk.ChunkSection");
            if (sectionClass == null) return;

            section_hasOnlyAir = findMethod(sectionClass, "hasOnlyAir", "isEmpty");
            if (section_hasOnlyAir != null) {
                CSLLogger.debug(() -> "[ModernNMS] Air-section skip enabled");
            }
        } catch (Throwable t) {
            CSLLogger.debug(() -> "[ModernNMS] Section palette init: " + t.getMessage());
        }
    }

    @Override
    protected boolean sectionMayContainTrackedBlocks(World world, int chunkX, int chunkZ, int sectionY) {
        if (chunk_getSection == null || nmsChunkHandle == null) return true;
        try {
            Object section = chunk_getSection.invoke(nmsChunkHandle, sectionY);
            if (section == null) return false;
            if (section_hasOnlyAir != null) {
                try {
                    Boolean isEmpty = (Boolean) section_hasOnlyAir.invoke(section);
                    if (Boolean.TRUE.equals(isEmpty)) return false;
                } catch (Throwable ignored) {}
            }
            return true;
        } catch (Throwable t) {
            return true; // safety: scan on error
        }
    }

    @Override
    protected void scanChunkSync(org.bukkit.Chunk chunk,
                                  com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord coord) {
        try {
            nmsChunkHandle = craftWorld_getHandle.invoke(chunk.getWorld());
        } catch (Throwable ignored) { nmsChunkHandle = null; }
        super.scanChunkSync(chunk, coord);
        nmsChunkHandle = null;
    }

    @Override
    protected Material getMaterialAtImpl(World world, int x, int y, int z) {
        try {
            Object serverLevel = craftWorld_getHandle.invoke(world);
            if (serverLevel == null) return null;
            Object blockPos = blockPosConstructor.newInstance(x, y, z);
            Object blockState = getBlockState.invoke(serverLevel, blockPos);
            if (blockState == null) return null;

            if (getBukkitMaterial != null) {
                try {
                    Object result = getBukkitMaterial.invoke(blockState);
                    if (result instanceof Material) return (Material) result;
                } catch (Exception ignored) {}
            }
            if (getBlock != null && craftMagicNumbers_getMaterial != null) {
                try {
                    Object block = getBlock.invoke(blockState);
                    Object result = craftMagicNumbers_getMaterial.invoke(null, block);
                    if (result instanceof Material) return (Material) result;
                } catch (Exception ignored) {}
            }
            return null;
        } catch (Exception e) {
            CSLLogger.debug(() -> "[ModernNMS] Error: " + e.getMessage());
            return null;
        }
    }

    @Override public boolean isSupported() { return initialized; }
    @Override public String getImplementationName() { return "ModernNMS"; }

    private static Class<?> loadClass(String... names) {
        for (String name : names) {
            try { return Class.forName(name); }
            catch (ClassNotFoundException ignored) {}
        }
        return null;
    }
    private static Method findMethod(Class<?> clazz, Class<?> paramType, String... names) {
        for (String name : names) {
            try { return clazz.getMethod(name, paramType); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
    private static Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try { return clazz.getMethod(name); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
}