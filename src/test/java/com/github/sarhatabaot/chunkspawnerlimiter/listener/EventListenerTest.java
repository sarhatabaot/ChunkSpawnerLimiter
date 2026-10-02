package com.github.sarhatabaot.chunkspawnerlimiter.listener;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.notification.NotificationService;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.Prevent;
import com.github.sarhatabaot.chunkspawnerlimiter.tracker.EntityChunkTracker;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @DisplayName("Should ignore creature spawns when creature watching is disabled")
    void shouldIgnoreCreatureSpawnsWhenCreatureWatchingIsDisabled() {
        PluginConfig pluginConfig = mock(PluginConfig.class);
        EventListener listener = new EventListener(
                mock(Plugin.class), pluginConfig, new CounterDataManager(),
                mock(NotificationService.class), mock(EntityChunkTracker.class));
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);

        listener.onEntitySpawn(event);

        verify(event, never()).getLocation();
        verify(event, never()).getEntity();
    }

    @Test
    @DisplayName("Should ignore generic entity spawns when entity watching is disabled")
    void shouldIgnoreGenericEntitySpawnsWhenEntityWatchingIsDisabled() {
        PluginConfig pluginConfig = mock(PluginConfig.class);
        EventListener listener = new EventListener(
                mock(Plugin.class), pluginConfig, new CounterDataManager(),
                mock(NotificationService.class), mock(EntityChunkTracker.class));
        EntitySpawnEvent event = mock(EntitySpawnEvent.class);

        listener.onEntitySpawn(event);

        verify(event, never()).getLocation();
        verify(event, never()).getEntity();
    }

    @Test
    @DisplayName("Should ignore vehicle creation when vehicle watching is disabled")
    void shouldIgnoreVehicleCreationWhenVehicleWatchingIsDisabled() {
        PluginConfig pluginConfig = mock(PluginConfig.class);
        EventListener listener = new EventListener(
                mock(Plugin.class), pluginConfig, new CounterDataManager(),
                mock(NotificationService.class), mock(EntityChunkTracker.class));
        VehicleCreateEvent event = mock(VehicleCreateEvent.class);

        listener.onVehicleCreate(event);

        verify(event, never()).getVehicle();
    }

    @Test
    @DisplayName("Should clean notification cooldowns when players quit")
    void shouldCleanNotificationCooldownsWhenPlayersQuit() {
        Plugin plugin = mock(Plugin.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(
                plugin, pluginConfig, new CounterDataManager(), notificationService, chunkTracker);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        Player player = mock(Player.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onPlayerQuit(event);

        verify(notificationService).cleanup(player);
    }

    @Test
    @DisplayName("Should replace a stale count before enforcing the spawn limit")
    void shouldReplaceStaleCountBeforeEnforcingSpawnLimit() {
        Plugin plugin = mock(Plugin.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);

        EntitySpawnEvent event = mock(EntitySpawnEvent.class);
        Entity spawningEntity = mock(Entity.class);
        Entity existingEntity = mock(Entity.class);
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Location location = mock(Location.class);

        when(event.getLocation()).thenReturn(location);
        when(event.getEntity()).thenReturn(spawningEntity);
        when(spawningEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(spawningEntity.getLocation()).thenReturn(location);
        when(existingEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(1);
        when(chunk.getZ()).thenReturn(2);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenReturn(new Entity[]{existingEntity, spawningEntity});

        when(pluginConfig.isWorldDisabled("world")).thenReturn(false);
        when(pluginConfig.isEntitySpawnWatch()).thenReturn(true);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);
        when(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(150);
        when(pluginConfig.shouldDelayEntityCountForCompatibility()).thenReturn(false);

        ChunkCoord coord = ChunkCoord.from(chunk);
        counterDataManager.getCounterData(coord).setEntityCount(EntityType.ZOMBIE, 150);

        listener.onEntitySpawn(event);

        assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.ZOMBIE)).isEqualTo(2);
        verify(notificationService, never()).notifyEntitiesBlocked(any(), any(), anyInt());
        verify(chunkTracker).recordEntry(spawningEntity);
    }

    @Test
    @DisplayName("Should not count entities removed before deferred compatibility finalization")
    void shouldNotCountEntitiesRemovedBeforeDeferredCompatibilityFinalization() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);

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
        when(pluginConfig.isEntitySpawnWatch()).thenReturn(true);
        when(pluginConfig.getResolvedEntityLimit(EntityType.COW)).thenReturn(5);
        when(pluginConfig.shouldDelayEntityCountForCompatibility()).thenReturn(true);

        when(entity.isValid()).thenReturn(false);

        listener.onEntitySpawn(event);

        assertThat(counterDataManager.getCounterData(ChunkCoord.from(chunk)).getEntityCount(EntityType.COW)).isZero();
        verify(notificationService, never()).notifyEntitiesBlocked(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Should directly remove deferred prevent violations")
    void shouldDirectlyRemoveDeferredPreventViolations() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);

        EntitySpawnEvent event = mock(EntitySpawnEvent.class);
        Entity spawningEntity = mock(Entity.class);
        Entity existingEntity = mock(Entity.class);
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        Location location = mock(Location.class);
        AtomicBoolean spawningEntityValid = new AtomicBoolean(true);

        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));

        when(event.getLocation()).thenReturn(location);
        when(event.getEntity()).thenReturn(spawningEntity);
        when(spawningEntity.getType()).thenReturn(EntityType.COW);
        when(spawningEntity.getWorld()).thenReturn(world);
        when(spawningEntity.getLocation()).thenReturn(location);
        when(spawningEntity.isValid()).thenAnswer(invocation -> spawningEntityValid.get());
        doAnswer(invocation -> {
            spawningEntityValid.set(false);
            return null;
        }).when(spawningEntity).remove();
        when(existingEntity.getType()).thenReturn(EntityType.COW);
        when(existingEntity.isValid()).thenReturn(true);
        when(existingEntity.getTicksLived()).thenReturn(100);
        when(location.getWorld()).thenReturn(world);
        when(location.getChunk()).thenReturn(chunk);
        when(world.getName()).thenReturn("world");
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(1);
        when(chunk.getZ()).thenReturn(2);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getEntities()).thenReturn(new Entity[]{existingEntity, spawningEntity});

        when(pluginConfig.isWorldDisabled("world")).thenReturn(false);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.COW)).thenReturn(true);
        when(pluginConfig.isEntitySpawnWatch()).thenReturn(true);
        when(pluginConfig.getResolvedEntityLimit(EntityType.COW)).thenReturn(1);
        when(pluginConfig.shouldDelayEntityCountForCompatibility()).thenReturn(true);
        when(pluginConfig.getRemovalMode()).thenReturn(new Prevent(null));

        listener.onEntitySpawn(event);

        ChunkCoord coord = ChunkCoord.from(chunk);
        assertThat(counterDataManager.getCounterData(coord).getEntityCount(EntityType.COW)).isEqualTo(1);
        verify(spawningEntity).remove();
        verify(event, never()).setCancelled(true);
        verify(chunkTracker).recordExit(spawningEntity);
        verify(chunkTracker, never()).recordEntry(spawningEntity);
    }

    @Test
    @DisplayName("Should ignore cancelled portal events")
    void shouldIgnoreCancelledPortalEvents() throws NoSuchMethodException {
        Plugin plugin = mock(Plugin.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);
        EntityPortalEvent event = mock(EntityPortalEvent.class);

        when(event.isCancelled()).thenReturn(true);

        listener.onEntityPortal(event);

        EventHandler annotation = EventListener.class
                .getDeclaredMethod("onEntityPortal", EntityPortalEvent.class)
                .getAnnotation(EventHandler.class);
        assertThat(annotation.priority()).isEqualTo(EventPriority.MONITOR);
        assertThat(annotation.ignoreCancelled()).isTrue();
        verify(plugin, never()).getServer();
        verify(chunkTracker, never()).recordExit(any());
        verify(chunkTracker, never()).recordEntry(any());
    }

    @Test
    @DisplayName("Should reconcile successful portal transitions on the next tick")
    void shouldReconcileSuccessfulPortalTransitionsOnTheNextTick() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PluginConfig pluginConfig = mock(PluginConfig.class);
        NotificationService notificationService = mock(NotificationService.class);
        CounterDataManager counterDataManager = new CounterDataManager();
        EntityChunkTracker chunkTracker = mock(EntityChunkTracker.class);
        EventListener listener = new EventListener(plugin, pluginConfig, counterDataManager, notificationService, chunkTracker);
        EntityPortalEvent event = mock(EntityPortalEvent.class);
        Entity entity = mock(Entity.class);
        Location sourceLocation = mock(Location.class);
        Location destinationLocation = mock(Location.class);
        Chunk sourceChunk = mock(Chunk.class);
        Chunk destinationChunk = mock(Chunk.class);
        World sourceWorld = mock(World.class);
        World destinationWorld = mock(World.class);
        UUID sourceWorldId = UUID.randomUUID();
        UUID destinationWorldId = UUID.randomUUID();

        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(event.getFrom()).thenReturn(sourceLocation);
        when(event.getEntity()).thenReturn(entity);
        when(sourceLocation.getWorld()).thenReturn(sourceWorld);
        when(sourceLocation.getChunk()).thenReturn(sourceChunk);
        when(sourceChunk.getWorld()).thenReturn(sourceWorld);
        when(sourceChunk.getX()).thenReturn(1);
        when(sourceChunk.getZ()).thenReturn(2);
        when(sourceWorld.getName()).thenReturn("source");
        when(sourceWorld.getUID()).thenReturn(sourceWorldId);
        when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        when(entity.isValid()).thenReturn(true);
        when(entity.getWorld()).thenReturn(destinationWorld);
        when(entity.getLocation()).thenReturn(destinationLocation);
        when(destinationLocation.getChunk()).thenReturn(destinationChunk);
        when(destinationChunk.getWorld()).thenReturn(destinationWorld);
        when(destinationChunk.getX()).thenReturn(3);
        when(destinationChunk.getZ()).thenReturn(4);
        when(destinationWorld.getName()).thenReturn("destination");
        when(destinationWorld.getUID()).thenReturn(destinationWorldId);
        when(pluginConfig.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);

        ChunkCoord sourceCoord = new ChunkCoord(sourceWorldId, 1, 2);
        ChunkCoord destinationCoord = new ChunkCoord(destinationWorldId, 3, 4);
        counterDataManager.getCounterData(sourceCoord).setEntityCount(EntityType.ZOMBIE, 1);

        listener.onEntityPortal(event);

        assertThat(counterDataManager.getCounterData(sourceCoord).getEntityCount(EntityType.ZOMBIE)).isEqualTo(1);
        assertThat(counterDataManager.getCounterData(destinationCoord).getEntityCount(EntityType.ZOMBIE)).isZero();

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        task.getValue().run();

        assertThat(counterDataManager.getCounterData(sourceCoord).getEntityCount(EntityType.ZOMBIE)).isZero();
        assertThat(counterDataManager.getCounterData(destinationCoord).getEntityCount(EntityType.ZOMBIE)).isEqualTo(1);
        verify(chunkTracker).recordEntry(entity);
        verify(chunkTracker, never()).recordExit(entity);
    }
}
