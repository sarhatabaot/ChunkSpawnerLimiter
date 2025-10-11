package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnforceKill implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "enforce-kill"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (event != null) {
            event.setCancelled(true);
        }
        // Enforce + kill logic combined

        // for kill, we need to queue check? to remove excess not just to prevent the event todo
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        // Enforce + remove block
    }
}
