package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.WorldReflection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for block scanners providing common scanning logic.
 * Subclasses implement the version-specific material retrieval.
 */
public abstract class AbstractBlockScanner implements BlockScanner {
    protected static final int CHUNK_SIZE = 16;
    
    protected final Plugin plugin;
    protected final PluginConfig config;
    protected final CounterDataManager counterManager;

    protected AbstractBlockScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        this.plugin = plugin;
        this.config = config;
        this.counterManager = counterManager;
    }

    /**
     * Template method for getting material at coordinates.
     * Subclasses implement the specific retrieval mechanism.
     */
    protected abstract Material getMaterialAtImpl(World world, int x, int y, int z);

    @Override
    @Nullable
    public final Material getMaterialAt(World world, int x, int y, int z) {
        try {
            return getMaterialAtImpl(world, x, y, z);
        } catch (Exception e) {
            CSLLogger.debug(() -> "[" + getImplementationName() + "] Error getting material at " +
                    x + "," + y + "," + z + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public final void scanChunk(Chunk chunk, ChunkCoord coord, boolean async) {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> scanChunkSync(chunk, coord));
        } else {
            scanChunkSync(chunk, coord);
        }
    }

    /**
     * Perform the actual chunk scan synchronously.
     * Only scans blocks that have configured limits to optimize performance.
     */
    protected void scanChunkSync(Chunk chunk, ChunkCoord coord) {
        final World world = chunk.getWorld();
        final int startX = chunk.getX() << 4;
        final int startZ = chunk.getZ() << 4;

        // Get safe Y bounds
        final int minY = WorldReflection.getWorldMinHeightSafe(world);
        final int maxY = world.getMaxHeight();

        CSLLogger.debug(() -> "[" + getImplementationName() + "] Scanning chunk " + coord + 
                " (Y: " + minY + " to " + maxY + ")");

        int blocksScanned = 0;
        int blocksFound = 0;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = minY; y < maxY; y++) {
                    blocksScanned++;
                    
                    Material material = getMaterialAt(world, startX + x, y, startZ + z);
                    
                    // Skip if we couldn't get the material or it's not configured
                    if (material == null || !config.hasResolvedBlockLimit(material)) {
                        continue;
                    }

                    // Increment counter for this material
                    counterManager.getCounterData(coord).incrementBlock(material);
                    blocksFound++;
                }
            }
        }

        final int finalBlocks = blocksFound;
        final int finalScanned = blocksScanned;
        CSLLogger.debug(() -> "[" + getImplementationName() + "] Chunk scan complete: " + 
                finalBlocks + " tracked blocks found (scanned " + finalScanned + " total)");
    }

    @Override
    public String toString() {
        return getImplementationName() + " (supported: " + isSupported() + ")";
    }
}
