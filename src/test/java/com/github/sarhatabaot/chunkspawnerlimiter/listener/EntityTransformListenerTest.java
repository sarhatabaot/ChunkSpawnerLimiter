package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Entity transform listener")
class EntityTransformListenerTest {

    @Test
    @DisplayName("Should dynamically register and update transformed entity counters")
    void shouldDynamicallyRegisterAndUpdateTransformedEntityCounters() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        Entity original = mock(Entity.class);
        Entity transformed = mock(Entity.class);
        Location location = mock(Location.class);
        Chunk chunk = mock(Chunk.class);
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 2, 3);

        when(original.getType()).thenReturn(EntityType.ZOMBIE);
        when(transformed.getType()).thenReturn(EntityType.SKELETON);
        when(original.getLocation()).thenReturn(location);
        when(transformed.getLocation()).thenReturn(location);
        when(original.getWorld()).thenReturn(world);
        when(transformed.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(2);
        when(chunk.getZ()).thenReturn(3);
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("world");
        when(pluginConfig.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.SKELETON)).thenReturn(true);
        counterDataManager.getCounterData(coord).setEntityCount(EntityType.ZOMBIE, 1);

        EntityTransformListener.register(pluginManager, plugin, pluginConfig,
                counterDataManager, chunkTracker, TestTransformEvent.class);

        ArgumentCaptor<EventExecutor> executor = ArgumentCaptor.forClass(EventExecutor.class);
        verify(pluginManager).registerEvent(eq(TestTransformEvent.class), any(Listener.class),
                eq(EventPriority.NORMAL), executor.capture(), eq(plugin));
        executor.getValue().execute(mock(Listener.class), new TestTransformEvent(original, transformed));

        assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isZero();
        assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.SKELETON)).isEqualTo(1);
        verify(chunkTracker).recordExit(original);
        verify(chunkTracker).recordEntry(transformed);
    }

    public static final class TestTransformEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Entity entity;
        private final Entity transformedEntity;

        private TestTransformEvent(Entity entity, Entity transformedEntity) {
            this.entity = entity;
            this.transformedEntity = transformedEntity;
        }

        public Entity getEntity() {
            return entity;
        }

        public Entity getTransformedEntity() {
            return transformedEntity;
        }

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }

        public static HandlerList getHandlerList() {
            return HANDLERS;
        }
    }
}
