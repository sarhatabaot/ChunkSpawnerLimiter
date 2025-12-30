package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.AbstractBlockScanner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * NMS block scanner for Minecraft 1.17+ using Mojang-mapped classes.
 * Supports modern package structure: net.minecraft.server.level, net.minecraft.core, etc.
 */
public class ModernNmsScanner extends AbstractBlockScanner {
    
    private final boolean initialized;
    
    // NMS classes (Mojang-mapped)
    private Class<?> serverLevelClass;
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

    public ModernNmsScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        super(plugin, config, counterManager);
        this.initialized = initialize();
    }

    private boolean initialize() {
        try {
            // CraftWorld.getHandle()
            Class<?> craftWorldClass = loadClass("org.bukkit.craftbukkit.CraftWorld");
            if (craftWorldClass != null) {
                craftWorld_getHandle = craftWorldClass.getMethod("getHandle");
            }

            // Modern NMS classes (Mojang-mapped)
            serverLevelClass = loadClass(
                "net.minecraft.server.level.ServerLevel",
                "net.minecraft.server.level.WorldServer"
            );

            blockPosClass = loadClass("net.minecraft.core.BlockPosition", "net.minecraft.core.BlockPos");
            
            blockStateClass = loadClass(
                "net.minecraft.world.level.block.state.IBlockData",
                "net.minecraft.world.level.block.state.BlockState"
            );

            if (serverLevelClass == null || blockPosClass == null) {
                return false;
            }

            // BlockPos constructor (int, int, int)
            try {
                blockPosConstructor = blockPosClass.getConstructor(int.class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                CSLLogger.debug(() -> "[ModernNMS] BlockPos(int,int,int) constructor not found");
                return false;
            }

            // getBlockState(BlockPos) method
            getBlockState = findMethod(serverLevelClass, blockPosClass, "getBlockState", "getType", "a");
            if (getBlockState == null) {
                return false;
            }

            // Try to find Paper's getBukkitMaterial() optimization
            if (blockStateClass != null) {
                try {
                    getBukkitMaterial = blockStateClass.getMethod("getBukkitMaterial");
                    CSLLogger.debug(() -> "[ModernNMS] Found Paper's getBukkitMaterial() optimization");
                } catch (NoSuchMethodException e) {
                    // Not Paper or method doesn't exist - that's okay
                    getBukkitMaterial = null;
                }

                // Fallback: getBlock() method
                try {
                    getBlock = blockStateClass.getMethod("getBlock");
                } catch (NoSuchMethodException e) {
                    try {
                        getBlock = blockStateClass.getMethod("b");
                    } catch (NoSuchMethodException e2) {
                        getBlock = null;
                    }
                }
            }

            // CraftMagicNumbers.getMaterial(Block) for conversion
            try {
                Class<?> craftMagicNumbers = loadClass("org.bukkit.craftbukkit.util.CraftMagicNumbers");
                Class<?> blockClass = loadClass("net.minecraft.world.level.block.Block");
                if (craftMagicNumbers != null && blockClass != null) {
                    craftMagicNumbers_getMaterial = craftMagicNumbers.getMethod("getMaterial", blockClass);
                }
            } catch (NoSuchMethodException e) {
                craftMagicNumbers_getMaterial = null;
            }

            CSLLogger.debug(() -> "[ModernNMS] Initialization successful");
            return true;

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ModernNMS] Initialization failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected Material getMaterialAtImpl(World world, int x, int y, int z) {
        if (!initialized) {
            return null;
        }

        try {
            // Get NMS ServerLevel from CraftWorld
            Object serverLevel = craftWorld_getHandle.invoke(world);
            if (serverLevel == null) {
                return null;
            }

            // Create BlockPos
            Object blockPos = blockPosConstructor.newInstance(x, y, z);

            // Get BlockState
            Object blockState = getBlockState.invoke(serverLevel, blockPos);
            if (blockState == null) {
                return null;
            }

            // Try Paper optimization first
            if (getBukkitMaterial != null) {
                try {
                    Object result = getBukkitMaterial.invoke(blockState);
                    if (result instanceof Material) {
                        return (Material) result;
                    }
                } catch (Exception ignored) {
                    // Fall through to next method
                }
            }

            // Try getBlock() -> CraftMagicNumbers
            if (getBlock != null && craftMagicNumbers_getMaterial != null) {
                try {
                    Object block = getBlock.invoke(blockState);
                    Object result = craftMagicNumbers_getMaterial.invoke(null, block);
                    if (result instanceof Material) {
                        return (Material) result;
                    }
                } catch (Exception ignored) {
                    // Fall through
                }
            }

            return null;

        } catch (Exception e) {
            CSLLogger.debug(() -> "[ModernNMS] Error getting material: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isSupported() {
        return initialized;
    }

    @Override
    public String getImplementationName() {
        return "ModernNMS";
    }

    // Helper methods

    private Class<?> loadClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
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
