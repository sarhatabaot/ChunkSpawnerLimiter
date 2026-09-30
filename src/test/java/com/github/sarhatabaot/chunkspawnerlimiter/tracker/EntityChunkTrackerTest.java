package com.github.sarhatabaot.chunkspawnerlimiter.tracker;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EntityChunkTracker Tests")
class EntityChunkTrackerTest {

    @Test
    @DisplayName("Should decrement the stored type when an entity disappears")
    void shouldDecrementStoredTypeWhenEntityDisappears() {
        CounterDataManager manager = new CounterDataManager();
        EntityChunkTracker tracker = new EntityChunkTracker(
                mock(Plugin.class), manager, entity -> entity.getType() == EntityType.ZOMBIE, 40L, false);
        World world = mock(World.class);
        Entity entity = entity(world, EntityType.ZOMBIE, 1, 2);
        ChunkCoord coord = ChunkCoord.from(world, 1, 2);
        manager.getCounterData(coord).setEntityCount(EntityType.ZOMBIE, 1);
        tracker.recordEntry(entity);

        tracker.reconcileEntities(List.of());

        assertThat(manager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isZero();
        assertThat(tracker.getTrackedCount()).isZero();
    }

    @Test
    @DisplayName("Should transfer a tracked entity between chunks")
    void shouldTransferEntityBetweenChunks() {
        CounterDataManager manager = new CounterDataManager();
        EntityChunkTracker tracker = new EntityChunkTracker(
                mock(Plugin.class), manager, entity -> entity.getType() == EntityType.ZOMBIE, 40L, false);
        World world = mock(World.class);
        Entity entity = entity(world, EntityType.ZOMBIE, 1, 2);
        ChunkCoord oldCoord = ChunkCoord.from(world, 1, 2);
        ChunkCoord newCoord = ChunkCoord.from(world, 3, 4);
        manager.getCounterData(oldCoord).setEntityCount(EntityType.ZOMBIE, 1);
        tracker.recordEntry(entity);

        Location movedLocation = mock(Location.class);
        Chunk movedChunk = chunk(world, 3, 4);
        when(movedLocation.getWorld()).thenReturn(world);
        when(movedLocation.getChunk()).thenReturn(movedChunk);
        when(entity.getLocation()).thenReturn(movedLocation);

        tracker.reconcileEntities(List.of(entity));

        assertThat(manager.getCounterData(oldCoord).getEntityCount(EntityType.ZOMBIE)).isZero();
        assertThat(manager.getCounterData(newCoord).getEntityCount(EntityType.ZOMBIE)).isOne();
    }

    @Test
    @DisplayName("Should forget unloaded chunks without recreating their counters")
    void shouldForgetUnloadedChunk() {
        CounterDataManager manager = new CounterDataManager();
        EntityChunkTracker tracker = new EntityChunkTracker(
                mock(Plugin.class), manager, entity -> entity.getType() == EntityType.ZOMBIE, 40L, false);
        World world = mock(World.class);
        Entity entity = entity(world, EntityType.ZOMBIE, 1, 2);
        ChunkCoord coord = ChunkCoord.from(world, 1, 2);
        tracker.recordEntry(entity);

        tracker.forgetChunk(coord);
        tracker.reconcileEntities(List.of());

        assertThat(tracker.getTrackedCount()).isZero();
        assertThat(manager.getCounterDataIfPresent(coord)).isNull();
    }

    @Test
    @DisplayName("Should not track ineligible players")
    void shouldNotTrackIneligiblePlayers() {
        CounterDataManager manager = new CounterDataManager();
        EntityChunkTracker tracker = new EntityChunkTracker(
                mock(Plugin.class), manager, entity -> !(entity instanceof Player), 40L, false);
        Player player = mock(Player.class);

        tracker.recordEntry(player);

        assertThat(tracker.getTrackedCount()).isZero();
    }

    private Entity entity(World world, EntityType type, int chunkX, int chunkZ) {
        Entity entity = mock(Entity.class);
        Location location = mock(Location.class);
        Chunk chunk = chunk(world, chunkX, chunkZ);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.getType()).thenReturn(type);
        when(entity.isValid()).thenReturn(true);
        when(entity.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        return entity;
    }

    private Chunk chunk(World world, int chunkX, int chunkZ) {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(chunkX);
        when(chunk.getZ()).thenReturn(chunkZ);
        return chunk;
    }
}
