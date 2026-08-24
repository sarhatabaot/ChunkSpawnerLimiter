package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression test for the spawn-egg duplication bug:
 * <p>
 * When a player used a villager spawn egg while at the limit, CSL would cancel
 * the spawn and then drop a NEW spawn egg on the ground. On modern Paper, the
 * player keeps their original egg too, resulting in an unlimited dupe exploit.
 * <p>
 * The fix: do not drop a refund egg. The egg is already handled by Bukkit/Paper.
 */
@DisplayName("EventListener — no egg refund")
class EventListenerNoEggRefundTest {

    @Test
    @DisplayName("Should not drop a refund egg when spawn is cancelled at the limit")
    void shouldNotDropRefundEggWhenSpawnIsCancelled() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);

        EventListener listener = new EventListener(
                plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);

        // Build a CreatureSpawnEvent for a villager spawned via SPAWNER_EGG
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        Villager villager = mock(Villager.class);
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Location location = mock(Location.class);

        when(event.getLocation()).thenReturn(location);
        when(event.getEntity()).thenReturn(villager);
        when(event.isCancelled()).thenReturn(true);
        when(event.getSpawnReason()).thenReturn(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);

        when(villager.getType()).thenReturn(EntityType.VILLAGER);
        when(villager.getWorld()).thenReturn(world);
        when(villager.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.dropItemNaturally(any(Location.class), any())).thenReturn(null);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(chunk.isLoaded()).thenReturn(true);

        when(pluginConfig.isWorldDisabled("world")).thenReturn(false);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.VILLAGER)).thenReturn(true);
        when(pluginConfig.getResolvedEntityLimit(EntityType.VILLAGER)).thenReturn(5);
        // Force the non-deferred path so the cancel happens immediately
        when(pluginConfig.shouldDelayEntityCountForCompatibility()).thenReturn(false);

        // Pre-fill the counter to the limit so the next spawn is blocked
        ChunkCoord coord = ChunkCoord.from(chunk);
        for (int i = 0; i < 5; i++) {
            counterDataManager.getCounterData(coord).incrementEntity(EntityType.VILLAGER);
        }

        listener.onEntitySpawn(event);

        // The fix: the plugin must NOT drop a refund egg on the ground.
        verify(world, never()).dropItemNaturally(any(Location.class), any());
    }
}