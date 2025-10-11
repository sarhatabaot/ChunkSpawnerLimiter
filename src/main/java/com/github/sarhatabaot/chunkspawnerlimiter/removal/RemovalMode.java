package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public sealed interface RemovalMode
        permits Prevent, Remove, Kill, Enforce, EnforceKill {

    @NotNull String getKey();

    void handleEntity(@NotNull Entity entity, @Nullable Cancellable event);
    void handleBlock(@NotNull Block block, @NotNull Cancellable event);

    // Singleton instances (like enum constants)
    Map<String, RemovalMode> MODES = Map.of(
            "prevent", new Prevent(),
            "remove", new Remove(),
            "kill", new Kill(),
            "enforce", new Enforce(),
            "enforce-kill", new EnforceKill()
    );

    static @NotNull RemovalMode fromString(@NotNull String mode) {
        return MODES.getOrDefault(mode.toLowerCase(), MODES.get("enforce"));
    }
}
