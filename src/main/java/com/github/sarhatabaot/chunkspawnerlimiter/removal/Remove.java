package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Remove implements RemovalMode {
    @Contract(pure = true)
    public @NotNull String getKey() { return "remove"; }

    @Override
    public void handleEntity(@NotNull Entity entity) {
        entity.remove();
    }

    @Override
    public void handleBlock(@NotNull Block block) {
        // for old behaviour this should act like prevent. We should probably pass the event instead. Or maybe both? Or a wrapper? todo
        block.setType(org.bukkit.Material.AIR);
    }
}
