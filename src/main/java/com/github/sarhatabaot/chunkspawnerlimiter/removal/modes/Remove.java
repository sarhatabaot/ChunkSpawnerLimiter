package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Remove implements RemovalMode {
    private final RemovalTaskManager removalTaskManager;
    public Remove(RemovalTaskManager removalTaskManager) {
        this.removalTaskManager = removalTaskManager;
    }

    @Contract(pure = true)
    public @NotNull String getKey() { return "remove"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        entity.remove();

        ChunkCoord coord = ChunkCoord.from(entity.getLocation().getChunk());
        removalTaskManager.queueChunkCheck(coord, getEntityRemovalAction());
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        block.setType(org.bukkit.Material.AIR);
    }

    @Override
    public Consumer<Entity> getEntityRemovalAction() {
        return e -> {
            if (e instanceof Player player) {
                player.setHealth(0);
            } else {
                e.remove();
            }
        };
    }
}
