package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Prevent implements RemovalMode {
    private static final Consumer<Entity> NO_OP_REMOVAL = entity -> { };
    private final RemovalTaskManager removalTaskManager;

    public Prevent(RemovalTaskManager removalTaskManager) {
        this.removalTaskManager = removalTaskManager;
    }

    @Contract(pure = true)
    public @NotNull String getKey() { return "prevent"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }

        if (entity instanceof Vehicle) {
            entity.remove();
            // entity.remove() does NOT fire EntityDeathEvent, so we must decrement manually
            removalTaskManager.getCounterDataManager().decrementEntityForRemoval(entity);
        }
    }

    @Override
    public void handleDeferredEntity(@NotNull Entity entity) {
        entity.remove();
    }

    @Override
    public boolean removesExistingEntities() {
        return false;
    }

    @Override
    public @NotNull Consumer<Entity> getEntityRemovalAction() {
        return NO_OP_REMOVAL;
    }

    @Override
    public void handleBlock(@NotNull Block block,@NotNull Cancellable event) {
        event.setCancelled(true);
    }
}
