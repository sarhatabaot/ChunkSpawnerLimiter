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

import java.util.Set;

/**
 * Abstract base class for block scanners providing common scanning logic.
 * <p>
 * Subclasses implement the version-specific material retrieval and palette
 * fast-path queries. The base class uses palette checks to skip sections that
 * contain no tracked blocks, dramatically reducing block-by-block scans.
 */
public abstract class AbstractBlockScanner implements BlockScanner {
    protected static final int CHUNK_SIZE = 16;

    protected final Plugin plugin;
    protected final PluginConfig config;
    protected final CounterDataManager counterManager;
    protected final Set<Material> trackedMaterials;

    protected AbstractBlockScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        this.plugin = plugin;
        this.config = config;
        this.counterManager = counterManager;
        this.trackedMaterials = config.getTrackedBlockMaterials();
    }

    /**
     * Template method for getting material at coordinates.
     * Subclasses implement the specific retrieval mechanism.
     */
    protected abstract Material getMaterialAtImpl(World world, int x, int y, int z);

    /**
     * Check whether a chunk section (16×16×16 sub-volume) may contain any
     * of the tracked block materials. This is a fast palette-level check.
     * <p>
     * Default implementation returns {@code true} (always scan); subclasses
     * should override with an NMS palette query for best performance.
     *
     * @param world   the world
     * @param chunkX  chunk X
     * @param chunkZ  chunk Z
     * @param sectionY the section index (not world Y; multiply by 16 to get block Y)
     * @return true if this section should be scanned block-by-block
     */
    protected boolean sectionMayContainTrackedBlocks(World world, int chunkX, int chunkZ, int sectionY) {
        return true; // Override in NMS subclasses
    }

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
     * Perform the actual chunk scan. Uses section-level palette checks to skip
     * empty sections and sections that don't contain any tracked blocks.
     */
    protected void scanChunkSync(Chunk chunk, ChunkCoord coord) {
        final World world = chunk.getWorld();
        final int startX = chunk.getX() << 4;
        final int startZ = chunk.getZ() << 4;

        final int minY = WorldReflection.getWorldMinHeightSafe(world);
        final int maxY = world.getMaxHeight();
        final int minSection = minY >> 4;
        final int maxSection = (maxY - 1) >> 4;

        CSLLogger.debug(() -> "[" + getImplementationName() + "] Scanning chunk " + coord +
                " (Y: " + minY + " to " + maxY + ", sections: " + minSection + "-" + maxSection + ")");

        int blocksScanned = 0;
        int blocksFound = 0;
        int sectionsSkipped = 0;

        for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
            // Fast palette check — skip sections that have no tracked blocks
            if (!sectionMayContainTrackedBlocks(world, chunk.getX(), chunk.getZ(), sectionY)) {
                sectionsSkipped++;
                continue;
            }

            int yStart = sectionY << 4;
            int yEnd = Math.min(yStart + 16, maxY);

            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    for (int y = yStart; y < yEnd; y++) {
                        blocksScanned++;

                        Material material = getMaterialAt(world, startX + x, y, startZ + z);

                        if (material == null || !config.hasResolvedBlockLimit(material)) {
                            continue;
                        }

                        counterManager.getCounterData(coord).incrementBlock(material);
                        blocksFound++;
                    }
                }
            }
        }

        final int finalBlocks = blocksFound;
        final int finalScanned = blocksScanned;
        final int finalSkipped = sectionsSkipped;
        CSLLogger.debug(() -> "[" + getImplementationName() + "] Chunk scan: " +
                finalBlocks + " tracked blocks, scanned " + finalScanned +
                " blocks, skipped " + finalSkipped + " sections");
    }

    @Override
    public String toString() {
        return getImplementationName() + " (supported: " + isSupported() + ")";
    }
}