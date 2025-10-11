package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Kill implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "kill"; }

    @Override
    public void handleEntity(@NotNull Entity entity, @Nullable Cancellable event) {
        if (entity instanceof LivingEntity living) {
            living.setHealth(0);
        } else {
            entity.remove();
        }
    }

    @Override
    public void handleBlock(@NotNull Block block, @NotNull Cancellable event) {
        block.breakNaturally();
    }
}
