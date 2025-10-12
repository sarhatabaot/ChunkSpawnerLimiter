package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Enforce implements RemovalMode {
    private final RemovalTaskManager removalTaskManager;

    public Enforce(RemovalTaskManager removalTaskManager) {
        this.removalTaskManager = removalTaskManager;
    }


    @Contract(pure = true)
    public @NotNull String getKey() {
        return "enforce";
    }


    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }

        if (entity instanceof Vehicle) {
            entity.remove();
        }

        ChunkCoord coord = ChunkCoord.from(entity.getLocation().getChunk());
        removalTaskManager.queueChunkCheck(coord, Entity::remove);
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        event.setCancelled(true);
    }
}
