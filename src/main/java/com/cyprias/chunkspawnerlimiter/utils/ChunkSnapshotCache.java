package com.cyprias.chunkspawnerlimiter.utils;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.ChunkSnapshot;
import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Hybrid cache for chunk block counts (scans low-limit blocks, caches high-limit blocks).
 */
public final class ChunkSnapshotCache {
    private final ChunkSpawnerLimiter plugin;
    private final ConcurrentMap<String, Map<Material, Integer>> materialCounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ChunkSnapshot> chunkSnapshots = new ConcurrentHashMap<>();

    public ChunkSnapshotCache(ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
    }

    private static String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * Gets a cached snapshot or creates a new one (thread-safe).
     */
    public ChunkSnapshot getSnapshot(Chunk chunk) {
        return chunkSnapshots.computeIfAbsent(getChunkKey(chunk), k -> chunk.getChunkSnapshot());
    }

    /**
     * Gets block count for a material, using cache for high-limit blocks.
     */
    public int getMaterialCount(Chunk chunk, Material material, int minY, int maxY, int limit) {
        // Use cache only for high-limit materials
        if (limit > plugin.getBlocksConfig().getMinLimitForCache()) {
            String key = getChunkKey(chunk);
            return materialCounts.getOrDefault(key, new HashMap<>()).getOrDefault(material, 0);
        }
        // Fall back to scanning for low-limit materials
        return countBlocksInChunk(getSnapshot(chunk), material, minY, maxY);
    }

    /**
     * Updates material count cache (only for high-limit blocks).
     */
    public void updateMaterialCount(Chunk chunk, Material material, int delta, int limit) {
        if (limit > plugin.getBlocksConfig().getMinLimitForCache()) { // Only cache if limit is high
            String key = getChunkKey(chunk);
            materialCounts.compute(key, (k, counts) -> {
                Map<Material, Integer> newCounts = (counts != null) ? new HashMap<>(counts) : new HashMap<>();
                newCounts.merge(material, delta, Integer::sum);
                return newCounts;
            });
        }
        // Always invalidate snapshot on changes
        invalidateSnapshot(chunk);
    }

    /**
     * Counts blocks in a chunk section (reused from your original method).
     */
    private int countBlocksInChunk(ChunkSnapshot snapshot, Material material, int minY, int maxY) {
        int count = 0;
        for (int y = minY; y < maxY; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (snapshot.getBlockType(x, y, z) == material) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Invalidates all cached data for a chunk.
     */
    public void invalidate(Chunk chunk) {
        String key = getChunkKey(chunk);
        materialCounts.remove(key);
        chunkSnapshots.remove(key);
    }

    /**
     * Only invalidates the snapshot (keeps material counts).
     */
    public void invalidateSnapshot(Chunk chunk) {
        chunkSnapshots.remove(getChunkKey(chunk));
    }

    /**
     * Clears the entire cache.
     */
    public void clear() {
        materialCounts.clear();
        chunkSnapshots.clear();
    }
}