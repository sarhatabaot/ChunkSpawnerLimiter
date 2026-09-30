package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("CounterDataManager Tests")
class CounterDataManagerTest {

    @Test
    @DisplayName("Should synchronize an entity type while excluding the spawning entity")
    void shouldSynchronizeEntityCountExcludingSpawn() {
        CounterDataManager manager = new CounterDataManager();
        Chunk chunk = mock(Chunk.class);
        World world = mock(World.class);
        Entity spawningEntity = mock(Entity.class);
        Entity zombie = mock(Entity.class);
        Entity skeleton = mock(Entity.class);

        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(3);
        when(chunk.getZ()).thenReturn(4);
        when(spawningEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(skeleton.getType()).thenReturn(EntityType.SKELETON);
        when(chunk.getEntities()).thenReturn(new Entity[]{spawningEntity, zombie, skeleton});

        ChunkCoord coord = ChunkCoord.from(chunk);
        manager.getCounterData(coord).setEntityCount(EntityType.ZOMBIE, 150);

        int actual = manager.synchronizeEntityCount(chunk, EntityType.ZOMBIE, spawningEntity);

        assertThat(actual).isOne();
        assertThat(manager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isOne();
    }

    @Test
    @DisplayName("Should not create a counter while decrementing an unloaded chunk")
    void shouldNotCreateCounterWhenDecrementingMissingChunk() {
        CounterDataManager manager = new CounterDataManager();
        ChunkCoord coord = new ChunkCoord(UUID.randomUUID(), 7, 8);

        manager.decrementEntityIfPresent(coord, EntityType.ZOMBIE);

        assertThat(manager.getCounterDataIfPresent(coord)).isNull();
    }

    @Test
    @DisplayName("Should apply entity eligibility while rescanning loaded chunks")
    void shouldApplyEntityEligibilityWhileRescanningLoadedChunks() {
        CounterDataManager manager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Player player = mock(Player.class);
        Entity zombie = mock(Entity.class);
        UUID worldId = UUID.randomUUID();

        when(world.getUID()).thenReturn(worldId);
        when(world.getLoadedChunks()).thenReturn(new Chunk[]{chunk});
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(3);
        when(chunk.getZ()).thenReturn(4);
        when(chunk.getEntities()).thenReturn(new Entity[]{player, zombie});
        when(player.getType()).thenReturn(EntityType.PLAYER);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));

            int rescanned = manager.rescanAllLoadedChunks(entity -> !(entity instanceof Player));

            ChunkCoord coord = ChunkCoord.from(chunk);
            assertThat(rescanned).isOne();
            assertThat(manager.getCounterData(coord).getEntityCount(EntityType.PLAYER)).isZero();
            assertThat(manager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isOne();
        }
    }
}
