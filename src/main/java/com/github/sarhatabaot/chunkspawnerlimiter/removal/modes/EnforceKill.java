package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnforceKill implements RemovalMode {
    private final RemovalTaskManager removalTaskManager;
    public EnforceKill(RemovalTaskManager removalTaskManager) {
        this.removalTaskManager = removalTaskManager;
    }


    @Contract(pure = true)
    public @NotNull String getKey() { return "enforce-kill"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }

        ChunkCoord coord = ChunkCoord.from(entity.getLocation().getChunk());
        removalTaskManager.queueChunkCheck(coord, e -> {
            if (entity instanceof LivingEntity living) {
                living.setHealth(0);
            } else {
                entity.remove();
            }
        });
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        event.setCancelled(true);
        //todo not sure we want to remove excess blocks, though that is what we stated we will do.
    }
}
