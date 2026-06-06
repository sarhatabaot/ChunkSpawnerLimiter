package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventListener Tests")
class EventListenerTest {

    @Test
    @DisplayName("Should not count entities removed before deferred compatibility finalization")
    void shouldNotCountEntitiesRemovedBeforeDeferredCompatibilityFinalization() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService);

        EntitySpawnEvent event = mock(EntitySpawnEvent.class);
        Entity entity = mock(Entity.class);
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Location location = mock(Location.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));

        when(event.getLocation()).thenReturn(location);
        when(event.getEntity()).thenReturn(entity);
        when(entity.getType()).thenReturn(EntityType.COW);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(1);
        when(chunk.getZ()).thenReturn(2);
        when(chunk.isLoaded()).thenReturn(true);

        when(pluginConfig.isWorldDisabled("world")).thenReturn(false);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.COW)).thenReturn(true);
        when(pluginConfig.getResolvedEntityLimit(EntityType.COW)).thenReturn(5);
        when(pluginConfig.shouldDelayEntityCountForCompatibility()).thenReturn(true);

        when(entity.isValid()).thenReturn(false);

        listener.onEntitySpawn(event);

        assertThat(counterDataManager.getCounterData(ChunkCoord.from(chunk)).getEntityCount(EntityType.COW)).isZero();
        verify(notificationService, never()).notifyEntitiesBlocked(any(), any(), anyInt());
    }
}
