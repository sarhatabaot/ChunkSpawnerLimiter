package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Defines a strategy for handling block and entity limit violations.
 * <p>
 * Each {@link RemovalMode} represents a distinct behavior, such as preventing,
 * removing, killing, or enforcing limits on entities or blocks.
 * <p>
 * Implementations are registered via {@link #setup(RemovalTaskManager)} and
 * retrieved using {@link #fromString(String)}.
 *
 * <h2>Thread safety</h2>
 * This interface is <strong>not thread-safe</strong> due to the use of a mutable
 * static {@link HashMap} for mode registration. It is expected to be initialized
 * during plugin startup on the main server thread.
 */
@NotThreadSafe
public sealed interface RemovalMode
        permits Prevent, Remove, Kill, Enforce, EnforceKill {

    /**
     * Returns the unique string key identifying this removal mode.
     *
     * @return the mode key (lowercase, human-readable)
     */
    @NotNull
    String getKey();

    /**
     * Handles an entity that violates a configured limit.
     *
     * @param entity the affected entity
     * @param event  the associated cancellable event, or {@code null} if none exists
     */
    void handleEntity(@NotNull Entity entity, @Nullable Cancellable event);

    /**
     * Handles a block that violates a configured limit.
     *
     * @param block the affected block
     * @param event the cancellable event associated with the block placement or action
     */
    void handleBlock(@NotNull Block block, @NotNull Cancellable event);

    /**
     * Returns the action used to remove or otherwise process entities for this mode.
     *
     * @return a consumer that performs the entity removal action
     */
    Consumer<Entity> getEntityRemovalAction();

    /**
     * Registry of all available removal modes, keyed by their string identifiers.
     * <p>
     * This map is populated during plugin initialization.
     */
    Map<String, RemovalMode> MODES = new HashMap<>();

    /**
     * Initializes and registers all available removal modes.
     * <p>
     * This method should be called once during plugin startup and is not safe
     * to call concurrently.
     *
     * @param removalTaskManager the task manager used by modes that require
     *                           scheduled or asynchronous removal logic
     */
    static void setup(@NotNull RemovalTaskManager removalTaskManager) {
        reload(removalTaskManager);
    }

    static void reload(@NotNull RemovalTaskManager removalTaskManager) {
        MODES.clear(); // Safe to clear and rebuild
        MODES.put("prevent", new Prevent());
        MODES.put("remove", new Remove(removalTaskManager));
        MODES.put("kill", new Kill(removalTaskManager));
        MODES.put("enforce", new Enforce(removalTaskManager));
        MODES.put("enforce-kill", new EnforceKill(removalTaskManager));
    }

    /**
     * Resolves a removal mode from its string representation.
     *
     * @param mode the mode name
     * @return the corresponding {@link RemovalMode}, or {@code "enforce"} if the
     *         provided mode is unknown
     */
    static @NotNull RemovalMode fromString(@NotNull String mode) {
        return MODES.getOrDefault(mode.toLowerCase(), MODES.get("enforce"));
    }
}
