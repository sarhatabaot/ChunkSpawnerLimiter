package com.github.sarhatabaot.chunkspawnerlimiter.chunk;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ChunkCoord(UUID worldUuid, int chunkX, int chunkZ) {

    // Create from World and chunk coordinates
    @Contract("_, _, _ -> new")
    public static @NotNull ChunkCoord from(@NotNull World world, int chunkX, int chunkZ) {
        return new ChunkCoord(world.getUID(), chunkX, chunkZ);
    }
    // Create from a Chunk object
    @Contract("_ -> new")
    public static @NotNull ChunkCoord from(@NotNull Chunk chunk) {
        return new ChunkCoord(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    // Create from a Location
    @Contract("_ -> new")
    public static @NotNull ChunkCoord from(@NotNull Location location) {
        return from(location.getChunk());
    }

    // Create from an Entity
    @Contract("_ -> new")
    public static @NotNull ChunkCoord from(@NotNull Entity entity) {
        return from(entity.getLocation());
    }

    // Get the World object (may return null if world is not loaded)
    public World getWorld() {
        return Bukkit.getWorld(worldUuid);
    }

    public @Nullable Chunk getChunk() {
        World world = getWorld();
        if (world == null || !world.isChunkLoaded(chunkX, chunkZ)) {
            return null;
        }
        return world.getChunkAt(chunkX, chunkZ);
    }

    // Check if this chunk is currently loaded
    public boolean isLoaded() {
        World world = getWorld();
        return world != null && world.isChunkLoaded(chunkX, chunkZ);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return String.format("ChunkCoord{world=%s, x=%d, z=%d}", worldUuid, chunkX, chunkZ);
    }
}
