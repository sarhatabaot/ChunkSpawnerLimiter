package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;

/**
 * Listens for {@code EntityDespawnEvent} via reflection so the plugin remains
 * compatible with versions that lack the event (pre-1.9 Spigot).
 * <p>
 * Uses {@link PluginManager#registerEvent(Class, org.bukkit.event.Listener, EventPriority, org.bukkit.plugin.EventExecutor, Plugin)}
 * via reflection to avoid a compile-time dependency on EntityDespawnEvent.
 */
public final class DespawnListener {

    private static final boolean SUPPORTED;
    private static Class<?> DESPAWN_EVENT_CLASS;

    static {
        boolean ok = false;
        try {
            DESPAWN_EVENT_CLASS = Class.forName("org.bukkit.event.entity.EntityDespawnEvent");
            ok = true;
        } catch (Throwable ignored) {}
        SUPPORTED = ok;
    }

    private DespawnListener() {}

    public static void registerIfSupported(Plugin plugin,
                                           PluginConfig pluginConfig,
                                           CounterDataManager counterDataManager,
                                           EntityChunkTracker chunkTracker) {
        if (!SUPPORTED) return;

        try {
            PluginManager pm = Bukkit.getPluginManager();
            Method registerEvent = PluginManager.class.getMethod("registerEvent",
                    Class.class, org.bukkit.event.Listener.class,
                    EventPriority.class, org.bukkit.plugin.EventExecutor.class,
                    Plugin.class);

            org.bukkit.plugin.EventExecutor executor = (ignoredListener, event) -> {
                try {
                    Method getEntity = event.getClass().getMethod("getEntity");
                    Object entity = getEntity.invoke(event);
                    Method getWorld = entity.getClass().getMethod("getWorld");
                    Object world = getWorld.invoke(entity);
                    Method getName = world.getClass().getMethod("getName");
                    String worldName = (String) getName.invoke(world);

                    if (pluginConfig.isWorldDisabled(worldName)) return;

                    Method getType = entity.getClass().getMethod("getType");
                    Object typeObj = getType.invoke(entity);
                    if (!(typeObj instanceof org.bukkit.entity.EntityType)) return;
                    org.bukkit.entity.EntityType type = (org.bukkit.entity.EntityType) typeObj;

                    if (!pluginConfig.hasResolvedEntityLimit(type)) return;

                    Method getLocation = entity.getClass().getMethod("getLocation");
                    org.bukkit.Location loc = (org.bukkit.Location) getLocation.invoke(entity);
                    ChunkCoord coord = ChunkCoord.from(loc);

                    counterDataManager.getCounterData(coord).decrementEntity(type);
                    if (entity instanceof org.bukkit.entity.Entity bukkitEntity) {
                        chunkTracker.recordExit(bukkitEntity);
                    }

                    CSLLogger.debug(() -> "Entity despawn: %s in %s"
                            .formatted(type.name(), coord));
                } catch (Throwable t) {
                    CSLLogger.debug(() -> "Despawn handler error: " + t.getMessage());
                }
            };

            registerEvent.invoke(pm, DESPAWN_EVENT_CLASS,
                    new org.bukkit.event.Listener() {}, // empty listener marker
                    EventPriority.NORMAL, executor, plugin);

            CSLLogger.debug(() -> "[DespawnListener] EntityDespawnEvent handler registered");
        } catch (Throwable t) {
            CSLLogger.debug(() -> "[DespawnListener] Failed to register: " + t.getMessage());
        }
    }
}