package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public sealed interface RemovalMode
        permits Prevent, Remove, Kill, Enforce, EnforceKill {

    @NotNull String getKey();

    void handleEntity(@NotNull Entity entity, @Nullable Cancellable event);
    void handleBlock(@NotNull Block block, @NotNull Cancellable event);

    Map<String, RemovalMode> MODES = new HashMap<>();

    static void setup(@NotNull RemovalTaskManager removalTaskManager) {
        MODES.put("prevent", new Prevent());
        MODES.put("remove", new Remove(removalTaskManager));
        MODES.put("kill", new Kill(removalTaskManager));
        MODES.put("enforce", new Enforce(removalTaskManager));
        MODES.put("enforce-kill", new EnforceKill(removalTaskManager));
    }

    static @NotNull RemovalMode fromString(@NotNull String mode) {
        return MODES.getOrDefault(mode.toLowerCase(), MODES.get("enforce"));
    }
}
