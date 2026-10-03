package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;

@DisplayName("CounterDataManager Tests")
class CounterDataManagerTest {

    @Test
    @DisplayName("Should batch loaded chunk rescans across scheduler ticks")
    void shouldBatchLoadedChunkRescansAcrossSchedulerTicks() {
        CounterDataManager manager = new CounterDataManager();
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        World world = mock(World.class);
        Chunk firstChunk = mock(Chunk.class);
        Chunk secondChunk = mock(Chunk.class);
        Entity firstEntity = mock(Entity.class);
        Entity secondEntity = mock(Entity.class);
        Queue<Runnable> scheduledTasks = new ArrayDeque<>();
        AtomicInteger completedChunks = new AtomicInteger(-1);
        UUID worldId = UUID.randomUUID();

        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(invocation -> {
            scheduledTasks.offer(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        when(world.getUID()).thenReturn(worldId);
        when(world.getLoadedChunks()).thenReturn(new Chunk[]{firstChunk, secondChunk});
        when(firstChunk.getWorld()).thenReturn(world);
        when(firstChunk.getX()).thenReturn(1);
        when(firstChunk.getZ()).thenReturn(1);
        when(firstChunk.getEntities()).thenReturn(new Entity[]{firstEntity});
        when(secondChunk.getWorld()).thenReturn(world);
        when(secondChunk.getX()).thenReturn(2);
        when(secondChunk.getZ()).thenReturn(2);
        when(secondChunk.getEntities()).thenReturn(new Entity[]{secondEntity});
        when(firstEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(secondEntity.getType()).thenReturn(EntityType.SKELETON);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));

            manager.rescanAllLoadedChunksBatched(plugin, entity -> true, 1, completedChunks::set);
            scheduledTasks.remove().run();

            verify(firstChunk).getEntities();
            verify(secondChunk, never()).getEntities();
            assertThat(completedChunks).hasValue(-1);

            while (!scheduledTasks.isEmpty()) {
                scheduledTasks.remove().run();
            }

            verify(secondChunk).getEntities();
            assertThat(completedChunks).hasValue(2);
        }
    }

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
