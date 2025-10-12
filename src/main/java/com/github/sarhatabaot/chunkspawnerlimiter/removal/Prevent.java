package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Prevent implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "prevent"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }

        if (entity instanceof Vehicle) {
            entity.remove();
        }
    }

    @Override
    public void handleBlock(@NotNull Block block,@NotNull Cancellable event) {
        event.setCancelled(true);
    }
}
