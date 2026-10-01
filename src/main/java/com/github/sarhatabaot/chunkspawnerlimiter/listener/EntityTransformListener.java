package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.Checks;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

public final class EntityTransformListener {
    private static final String EVENT_CLASS_NAME =
            "com.destroystokyo.paper.event.entity.EntityTransformEvent";

    private EntityTransformListener() {
    }

    public static void registerIfSupported(Plugin plugin, PluginConfig pluginConfig,
                                           CounterDataManager counterDataManager,
                                           EntityChunkTracker chunkTracker) {
        try {
            Class<? extends Event> eventClass = Class.forName(EVENT_CLASS_NAME).asSubclass(Event.class);
            register(Bukkit.getPluginManager(), plugin, pluginConfig, counterDataManager, chunkTracker, eventClass);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            CSLLogger.debug(() -> "[EntityTransformListener] Failed to register: " + throwable.getMessage());
        }
    }

    static void register(PluginManager pluginManager, Plugin plugin, PluginConfig pluginConfig,
                         CounterDataManager counterDataManager, EntityChunkTracker chunkTracker,
                         Class<? extends Event> eventClass) throws ReflectiveOperationException {
        Method getEntity = eventClass.getMethod("getEntity");
        Method getTransformedEntity = eventClass.getMethod("getTransformedEntity");
        Listener listener = new Listener() { };
        EventExecutor executor = (ignored, event) -> {
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
                return;
            }

            try {
                Entity original = (Entity) getEntity.invoke(event);
                Entity transformed = (Entity) getTransformedEntity.invoke(event);
                handleTransform(original, transformed, pluginConfig, counterDataManager, chunkTracker);
            } catch (Throwable throwable) {
                CSLLogger.debug(() -> "Entity transform handler error: " + throwable.getMessage());
            }
        };

        pluginManager.registerEvent(eventClass, listener, EventPriority.NORMAL, executor, plugin);
        CSLLogger.debug(() -> "[EntityTransformListener] EntityTransformEvent handler registered");
    }

    private static void handleTransform(Entity original, Entity transformed, PluginConfig pluginConfig,
                                        CounterDataManager counterDataManager,
                                        EntityChunkTracker chunkTracker) {
        EntityType oldType = original.getType();
        EntityType newType = transformed.getType();
        ChunkCoord oldCoord = ChunkCoord.from(original);
        ChunkCoord newCoord = ChunkCoord.from(transformed);
        boolean originalTracked = !pluginConfig.isWorldDisabled(original.getWorld().getName())
                && Checks.shouldTrackEntity(original, pluginConfig);
        boolean transformedTracked = !pluginConfig.isWorldDisabled(transformed.getWorld().getName())
                && Checks.shouldTrackEntity(transformed, pluginConfig);
        boolean counterChanged = oldType != newType || !oldCoord.equals(newCoord)
                || originalTracked != transformedTracked;

        if (counterChanged && originalTracked) {
            counterDataManager.decrementEntityIfPresent(oldCoord, oldType);
        }
        chunkTracker.recordExit(original);

        if (counterChanged && transformedTracked) {
            counterDataManager.getCounterData(newCoord).incrementEntity(newType);
        }
        chunkTracker.recordEntry(transformed);

        CSLLogger.debug(() -> "Entity transform: %s→%s from %s to %s"
                .formatted(oldType.name(), newType.name(), oldCoord, newCoord));
    }
}
