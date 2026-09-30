package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
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

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Removal task manager")
class RemovalTaskManagerTest {

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
