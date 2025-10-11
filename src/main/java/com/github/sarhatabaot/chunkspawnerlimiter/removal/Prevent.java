package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Prevent implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "prevent"; }

    @Override
    public void handleEntity(@NotNull Entity entity) {
        // Do nothing — entity will not spawn or will be cancelled upstream
    }

    @Override
    public void handleBlock(@NotNull Block block) {
        // Prevent placement — could log or cancel placement event
    }
}
