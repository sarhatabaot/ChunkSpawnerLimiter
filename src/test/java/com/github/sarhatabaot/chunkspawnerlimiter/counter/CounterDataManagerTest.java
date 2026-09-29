package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
}
