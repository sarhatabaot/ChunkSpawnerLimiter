package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.AbstractBlockScanner;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Fallback block scanner using pure Bukkit API.
 * This implementation is slow but always works on any server version.
 * Used when NMS reflection fails or is unavailable.
 */
public class BukkitBlockScanner extends AbstractBlockScanner {

    public BukkitBlockScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        super(plugin, config, counterManager);
    }

    @Override
    protected Material getMaterialAtImpl(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType();
    }

    @Override
    public boolean isSupported() {
        return true; // Always supported
    }

    @Override
    public String getImplementationName() {
        return "Bukkit";
    }
}
