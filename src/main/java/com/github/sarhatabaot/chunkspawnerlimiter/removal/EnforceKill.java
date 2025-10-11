package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class EnforceKill implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "enforce-kill"; }

    @Override
    public void handleEntity(@NotNull Entity entity) {
        // Enforce + kill logic combined
    }

    @Override
    public void handleBlock(@NotNull Block block) {
        // Enforce + remove block
    }
}
