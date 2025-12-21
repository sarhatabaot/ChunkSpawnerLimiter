package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for scanning chunks and retrieving block materials.
 * Implementations may use NMS, reflection, or pure Bukkit API.
 */
public interface BlockScanner {
    
    /**
     * Get the material of a block at specific coordinates.
     * 
     * @param world the world containing the block
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the material at the coordinates, or null if unable to determine
     */
    @Nullable Material getMaterialAt(World world, int x, int y, int z);
    
    /**
     * Scan an entire chunk and update block counters.
     * 
     * @param chunk the chunk to scan
     * @param coord the chunk coordinates
     * @param async whether to perform the scan asynchronously
     */
    void scanChunk(Chunk chunk, ChunkCoord coord, boolean async);
    
    /**
     * Check if this scanner implementation is supported on the current server.
     * 
     * @return true if this scanner can be used, false otherwise
     */
    boolean isSupported();
    
    /**
     * Get the name of this scanner implementation for logging purposes.
     * 
     * @return the scanner implementation name (e.g., "ModernNMS", "Bukkit", etc.)
     */
    String getImplementationName();
}
