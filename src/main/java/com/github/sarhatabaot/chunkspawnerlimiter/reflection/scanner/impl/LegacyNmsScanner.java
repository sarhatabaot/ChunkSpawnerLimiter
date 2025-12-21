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
 * NMS block scanner for Minecraft 1.8.8-1.12 using legacy NMS structure.
 * Package structure: net.minecraft.server.v1_12_R1.*
 * Similar to Spigot scanner but with older NMS API differences.
 */
public class LegacyNmsScanner extends AbstractBlockScanner {
    
    private final boolean initialized;
    private final String versionSuffix;
    
    // NMS classes (legacy with version suffix)
    private Class<?> worldClass;
    private Class<?> blockPositionClass;
    private Class<?> iBlockDataClass;
    
    // Methods
    private Method craftWorld_getHandle;
    private Method getType; // World.getType(BlockPosition)
    private Method getBlock; // IBlockData.getBlock()
    private Method craftMagicNumbers_getMaterial;

    // Constructor
    private Constructor<?> blockPositionConstructor;

    public LegacyNmsScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        super(plugin, config, counterManager);
        this.versionSuffix = MinecraftVersion.detectVersionSuffix();
        this.initialized = initialize();
    }

    private boolean initialize() {
        if (versionSuffix == null || versionSuffix.isEmpty()) {
            CSLLogger.debug(() -> "[LegacyNMS] No version suffix detected");
            return false;
        }

        try {
            CSLLogger.debug(() -> "[LegacyNMS] Detected version suffix: " + versionSuffix);

            // CraftWorld.getHandle()
            Class<?> craftWorldClass = loadClass("org.bukkit.craftbukkit." + versionSuffix + ".CraftWorld");
            if (craftWorldClass != null) {
                craftWorld_getHandle = craftWorldClass.getMethod("getHandle");
            } else {
                return false;
            }

            // NMS classes with version suffix (legacy structure)
            worldClass = loadClass("net.minecraft.server." + versionSuffix + ".World");
            
            blockPositionClass = loadClass("net.minecraft.server." + versionSuffix + ".BlockPosition");
            
            iBlockDataClass = loadClass("net.minecraft.server." + versionSuffix + ".IBlockData");

            if (worldClass == null || blockPositionClass == null || iBlockDataClass == null) {
                CSLLogger.debug(() -> "[LegacyNMS] Failed to load required NMS classes");
                return false;
            }

            // BlockPosition constructor (int, int, int)
            try {
                blockPositionConstructor = blockPositionClass.getConstructor(int.class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                // Try alternative signatures for very old versions
                CSLLogger.debug(() -> "[LegacyNMS] BlockPosition(int,int,int) constructor not found");
                return false;
            }

            // World.getType(BlockPosition) method
            // In legacy versions, this might be called differently
            getType = findMethod(worldClass, blockPositionClass, "getType", "a");
            if (getType == null) {
                CSLLogger.debug(() -> "[LegacyNMS] getType method not found");
                return false;
            }

            // IBlockData.getBlock() method
            try {
                getBlock = iBlockDataClass.getMethod("getBlock");
            } catch (NoSuchMethodException e) {
                // Try obfuscated name
                try {
                    getBlock = iBlockDataClass.getMethod("b");
                } catch (NoSuchMethodException e2) {
                    CSLLogger.debug(() -> "[LegacyNMS] getBlock method not found");
                    getBlock = null;
                }
            }

            // CraftMagicNumbers.getMaterial(Block) for conversion
            try {
                Class<?> craftMagicNumbers = loadClass("org.bukkit.craftbukkit." + versionSuffix + ".util.CraftMagicNumbers");
                Class<?> blockClass = loadClass("net.minecraft.server." + versionSuffix + ".Block");
                if (craftMagicNumbers != null && blockClass != null) {
                    craftMagicNumbers_getMaterial = craftMagicNumbers.getMethod("getMaterial", blockClass);
                }
            } catch (NoSuchMethodException e) {
                CSLLogger.debug(() -> "[LegacyNMS] CraftMagicNumbers.getMaterial not found");
                craftMagicNumbers_getMaterial = null;
            }

            CSLLogger.debug(() -> "[LegacyNMS] Initialization successful for " + versionSuffix);
            return true;

        } catch (Exception e) {
            Bukkit.getLogger().warning("[LegacyNMS] Initialization failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected Material getMaterialAtImpl(World world, int x, int y, int z) {
        if (!initialized) {
            return null;
        }

        try {
            // Get NMS World from CraftWorld
            Object nmsWorld = craftWorld_getHandle.invoke(world);
            if (nmsWorld == null) {
                return null;
            }

            // Create BlockPosition
            Object blockPosition = blockPositionConstructor.newInstance(x, y, z);

            // Get IBlockData
            Object iBlockData = getType.invoke(nmsWorld, blockPosition);
            if (iBlockData == null) {
                return null;
            }

            // Get Block from IBlockData
            if (getBlock != null && craftMagicNumbers_getMaterial != null) {
                try {
                    Object block = getBlock.invoke(iBlockData);
                    Object result = craftMagicNumbers_getMaterial.invoke(null, block);
                    if (result instanceof Material) {
                        return (Material) result;
                    }
                } catch (Exception e) {
                    CSLLogger.debug(() -> "[LegacyNMS] Material conversion failed: " + e.getMessage());
                }
            }

            return null;

        } catch (Exception e) {
            CSLLogger.debug(() -> "[LegacyNMS] Error getting material: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isSupported() {
        return initialized;
    }

    @Override
    public String getImplementationName() {
        return "LegacyNMS" + (versionSuffix != null ? " (" + versionSuffix + ")" : "");
    }

    // Helper methods

    private Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Method findMethod(Class<?> clazz, Class<?> paramType, String... names) {
        for (String name : names) {
            try {
                return clazz.getMethod(name, paramType);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
