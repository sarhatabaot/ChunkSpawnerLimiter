package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Enforce implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "enforce"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }
        // Enforce rules (e.g. remove only if limit exceeded)
    }

    @Override
    public void handleBlock(@NotNull Block block,@NotNull Cancellable event) {
        // Similar enforcement logic for blocks
    }
}
