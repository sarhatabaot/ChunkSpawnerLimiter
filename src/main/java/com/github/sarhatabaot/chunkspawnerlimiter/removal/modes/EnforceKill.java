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

import java.util.function.Consumer;

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
        removalTaskManager.queueChunkCheck(coord, getEntityRemovalAction());
    }

    @Contract(pure = true)
    @Override
    public @NotNull Consumer<Entity> getEntityRemovalAction() {
        return e -> {
            if (e instanceof LivingEntity living) {
                living.setHealth(0);
            } else {
                e.remove();
            }
        };
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        event.setCancelled(true);
    }
}
