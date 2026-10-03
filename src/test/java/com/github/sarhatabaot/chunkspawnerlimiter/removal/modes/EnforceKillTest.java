package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.RemovalTaskManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Enforce-kill tests")
class EnforceKillTest {

    @Test
    @DisplayName("Should kill living entities without removing them first")
    void shouldKillLivingEntitiesWithoutRemovingThemFirst() {
        RemovalTaskManager removalTaskManager = mock(RemovalTaskManager.class);
        LivingEntity entity = mock(LivingEntity.class);
        Cancellable event = mock(Cancellable.class);
        ChunkCoord coord = configureLocation(entity);
        EnforceKill enforceKill = new EnforceKill(removalTaskManager);

        enforceKill.handleEntity(entity, event);

        verify(event).setCancelled(true);
        verify(entity).setHealth(0);
        verify(entity, never()).remove();
        verify(removalTaskManager, never()).getCounterDataManager();
        verify(removalTaskManager).queueChunkCheck(eq(coord), any());
    }

    @Test
    @DisplayName("Should directly remove and account for non-living entities once")
    void shouldDirectlyRemoveAndAccountForNonLivingEntitiesOnce() {
        RemovalTaskManager removalTaskManager = mock(RemovalTaskManager.class);
        CounterDataManager counterDataManager = mock(CounterDataManager.class);
        Entity entity = mock(Entity.class);
        ChunkCoord coord = configureLocation(entity);
        EnforceKill enforceKill = new EnforceKill(removalTaskManager);

        when(removalTaskManager.getCounterDataManager()).thenReturn(counterDataManager);

        enforceKill.handleEntity(entity, null);

        verify(entity).remove();
        verify(counterDataManager).decrementEntityForRemoval(entity);
        verify(removalTaskManager).queueChunkCheck(eq(coord), any());
    }

    private static ChunkCoord configureLocation(Entity entity) {
        Location location = mock(Location.class);
        Chunk chunk = mock(Chunk.class);
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();

        when(entity.getLocation()).thenReturn(location);
        when(location.getChunk()).thenReturn(chunk);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(3);
        when(chunk.getZ()).thenReturn(4);
        when(world.getUID()).thenReturn(worldId);

        return new ChunkCoord(worldId, 3, 4);
    }
}
