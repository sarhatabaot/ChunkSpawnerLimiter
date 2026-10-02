package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.RaidReflection;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Removal task manager")
class RemovalTaskManagerTest {

    @Test
    @DisplayName("Should collect raid membership once per chunk inspection")
    void shouldCollectRaidMembershipOncePerChunkInspection() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Entity raider = mock(Entity.class);
        Entity removable = mock(Entity.class);
        UUID raiderId = UUID.randomUUID();
        UUID removableId = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 3, 4);
        @SuppressWarnings("unchecked")
        Consumer<Entity> removalAction = mock(Consumer.class);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(config.getResolvedEntityTypes()).thenReturn(Set.of(EntityType.ZOMBIE));
        when(config.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);
        when(config.getResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(0);
        when(config.shouldPreserveRaidEntities()).thenReturn(true);
        when(config.getIgnoreMetadata()).thenReturn(List.of());
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(chunk);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenReturn(new Entity[]{raider, removable});
        when(raider.getType()).thenReturn(EntityType.ZOMBIE);
        when(raider.getUniqueId()).thenReturn(raiderId);
        when(raider.isValid()).thenReturn(true);
        when(removable.getType()).thenReturn(EntityType.ZOMBIE);
        when(removable.getUniqueId()).thenReturn(removableId);
        when(removable.isValid()).thenReturn(true);
        Checks.setup(config);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<RaidReflection> raids = mockStatic(RaidReflection.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            raids.when(() -> RaidReflection.getActiveRaiderUuids(world)).thenReturn(Set.of(raiderId));
            RemovalTaskManager manager = new RemovalTaskManager(
                    plugin, counterDataManager, config, System::currentTimeMillis, false);

            manager.processChunk(coord, removalAction);

            raids.verify(() -> RaidReflection.getActiveRaiderUuids(world), times(1));
            verify(removalAction, never()).accept(raider);
            verify(removalAction).accept(removable);
        }
    }

    @Test
    @DisplayName("Should enforce a newly configured type on its first inspection")
    void shouldEnforceANewlyConfiguredTypeOnItsFirstInspection() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Entity firstEntity = mock(Entity.class);
        Entity secondEntity = mock(Entity.class);
        List<Entity> entities = new ArrayList<>(List.of(firstEntity, secondEntity));
        AtomicInteger removals = new AtomicInteger();
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 3, 4);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(config.getResolvedEntityTypes()).thenReturn(Set.of(EntityType.ZOMBIE));
        when(config.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);
        when(config.getResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(1);
        when(config.getIgnoreMetadata()).thenReturn(List.of());
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(chunk);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenAnswer(invocation -> entities.toArray(Entity[]::new));
        for (Entity entity : entities) {
            when(entity.getType()).thenReturn(EntityType.ZOMBIE);
            when(entity.isValid()).thenReturn(true);
        }
        Checks.setup(config);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            RemovalTaskManager manager = new RemovalTaskManager(
                    plugin, counterDataManager, config, System::currentTimeMillis, false);

            manager.processChunk(coord, entity -> {
                entities.remove(entity);
                removals.incrementAndGet();
            });

            assertThat(removals).hasValue(1);
            assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should preserve queued chunks beyond the per-tick limit")
    void shouldPreserveQueuedChunksBeyondThePerTickLimit() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk firstChunk = mock(Chunk.class);
        Chunk secondChunk = mock(Chunk.class);
        Chunk thirdChunk = mock(Chunk.class);
        UUID worldId = UUID.randomUUID();
        ChunkCoord firstCoord = new ChunkCoord(worldId, 1, 0);
        ChunkCoord secondCoord = new ChunkCoord(worldId, 2, 0);
        ChunkCoord thirdCoord = new ChunkCoord(worldId, 3, 0);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(config.getInspectionMaxChunksPerTick()).thenReturn(2);
        when(world.isChunkLoaded(1, 0)).thenReturn(true);
        when(world.isChunkLoaded(2, 0)).thenReturn(true);
        when(world.isChunkLoaded(3, 0)).thenReturn(true);
        when(world.getChunkAt(1, 0)).thenReturn(firstChunk);
        when(world.getChunkAt(2, 0)).thenReturn(secondChunk);
        when(world.getChunkAt(3, 0)).thenReturn(thirdChunk);
        for (Chunk chunk : List.of(firstChunk, secondChunk, thirdChunk)) {
            when(chunk.isLoaded()).thenReturn(true);
            when(chunk.getEntities()).thenReturn(new Entity[0]);
        }

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            RemovalTaskManager manager = new RemovalTaskManager(
                    plugin, counterDataManager, config, System::currentTimeMillis, false);
            manager.queueChunkCheck(firstCoord, entity -> { });
            manager.queueChunkCheck(secondCoord, entity -> { });
            manager.queueChunkCheck(thirdCoord, entity -> { });

            manager.processQueue();

            verify(firstChunk).getEntities();
            verify(secondChunk).getEntities();
            verify(thirdChunk, never()).getEntities();

            manager.processQueue();

            verify(thirdChunk).getEntities();
        }
    }

    @Test
    @DisplayName("Should continue periodic inspections until removed")
    void shouldContinuePeriodicInspectionsUntilRemoved() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        AtomicLong clock = new AtomicLong(1_000L);
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 3, 4);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(world.getName()).thenReturn("world");
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(chunk);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenReturn(new Entity[0]);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            RemovalTaskManager manager = new RemovalTaskManager(
                    plugin, counterDataManager, config, clock::get, false);

            manager.scheduleRecheck(coord, entity -> { }, 1L);
            clock.set(1_999L);
            manager.processQueue();
            verify(chunk, never()).getEntities();

            clock.set(2_000L);
            manager.processQueue();
            clock.set(3_000L);
            manager.processQueue();
            verify(chunk, times(2)).getEntities();

            manager.removeChunkRecheck(coord);
            clock.set(4_000L);
            manager.processQueue();
            verify(chunk, times(2)).getEntities();
        }
    }

    @Test
    @DisplayName("Should reconcile counters after mixed removal accounting")
    void shouldReconcileCountersAfterMixedRemovalAccounting() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Entity deathEventEntity = mock(Entity.class);
        Entity directlyRemovedEntity = mock(Entity.class);
        Entity remainingEntity = mock(Entity.class);
        List<Entity> entities = new ArrayList<>(List.of(deathEventEntity, directlyRemovedEntity, remainingEntity));
        AtomicInteger removals = new AtomicInteger();
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 3, 4);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(config.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);
        when(config.getResolvedEntityTypes()).thenReturn(Set.of(EntityType.ZOMBIE));
        when(config.getResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(1);
        when(config.getIgnoreMetadata()).thenReturn(List.of());
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(chunk);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenAnswer(invocation -> entities.toArray(Entity[]::new));
        for (Entity entity : entities) {
            when(entity.getType()).thenReturn(EntityType.ZOMBIE);
            when(entity.isValid()).thenReturn(true);
        }
        counterDataManager.getCounterData(coord).setEntityCount(EntityType.ZOMBIE, 3);
        Checks.setup(config);

        Consumer<Entity> removalAction = entity -> {
            entities.remove(entity);
            if (entity == deathEventEntity) {
                counterDataManager.getCounterData(coord).decrementEntity(EntityType.ZOMBIE);
            }
            removals.incrementAndGet();
        };

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            RemovalTaskManager manager = new RemovalTaskManager(plugin, counterDataManager, config);

            manager.processChunk(coord, removalAction);

            assertThat(removals).hasValue(2);
            assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Should not remove players when player killing is disabled")
    void shouldNotRemovePlayersWhenPlayerKillingIsDisabled() {
        ChunkSpawnerLimiter plugin = mock(ChunkSpawnerLimiter.class);
        PluginConfig config = mock(PluginConfig.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Player player = mock(Player.class);
        @SuppressWarnings("unchecked")
        Consumer<Entity> removalAction = mock(Consumer.class);
        UUID worldId = UUID.randomUUID();
        ChunkCoord coord = new ChunkCoord(worldId, 3, 4);

        when(config.isNmsEntityCount()).thenReturn(false);
        when(config.hasResolvedEntityLimit(EntityType.PLAYER)).thenReturn(true);
        when(config.getResolvedEntityLimit(EntityType.PLAYER)).thenReturn(0);
        when(config.isKillPlayers()).thenReturn(false);
        when(world.isChunkLoaded(3, 4)).thenReturn(true);
        when(world.getChunkAt(3, 4)).thenReturn(chunk);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenReturn(new Entity[]{player});
        when(player.getType()).thenReturn(EntityType.PLAYER);
        counterDataManager.getCounterData(coord).setEntityCount(EntityType.PLAYER, 1);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld(worldId)).thenReturn(world);
            RemovalTaskManager manager = new RemovalTaskManager(plugin, counterDataManager, config);

            manager.processChunk(coord, removalAction);

            assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.PLAYER)).isZero();
            verify(removalAction, never()).accept(player);
        }
    }
}
