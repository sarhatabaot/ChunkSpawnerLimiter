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

public final class Kill implements RemovalMode {
    private final RemovalTaskManager removalTaskManager;
    public Kill(RemovalTaskManager removalTaskManager) {
        this.removalTaskManager = removalTaskManager;
    }

    @Contract(pure = true)
    public @NotNull String getKey() { return "kill"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        final Consumer<Entity> action = getEntityRemovalAction();

        action.accept(entity);

        ChunkCoord coord = ChunkCoord.from(entity.getLocation().getChunk());
        removalTaskManager.queueChunkCheck(coord, action);
    }

    @Override
    public Consumer<Entity> getEntityRemovalAction() {
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
        block.breakNaturally();
    }
}
