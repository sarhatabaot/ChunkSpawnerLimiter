package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SnapshotBlockScanner Tests")
class SnapshotBlockScannerTest {

    @Test
    @DisplayName("Should release failed captures for a deferred retry")
    void shouldReleaseFailedCapturesForADeferredRetry() {
        Plugin plugin = mock(Plugin.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager manager = new CounterDataManager();
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        World world = world();
        Chunk chunk = chunk(world, 0, 0);
        ChunkCoord coord = ChunkCoord.from(world, 0, 0);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(config.getTrackedBlockMaterials()).thenReturn(Set.of(Material.DIAMOND_BLOCK));
        when(chunk.getChunkSnapshot(false, false, false)).thenThrow(new IllegalStateException("capture failed"));
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenReturn(task);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            SnapshotBlockScanner scanner = new SnapshotBlockScanner(plugin, config, manager, 2);

            scanner.scanChunk(chunk, coord, true);

            assertThat(scanner.hasGeneration(coord)).isFalse();
            assertThat(scanner.isWorkerRunning()).isFalse();
            verify(scheduler).runTask(eq(plugin), any(Runnable.class));
        }
    }

    @Test
    @DisplayName("Should skip empty snapshot sections")
    void shouldSkipEmptySnapshotSections() {
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);
        when(snapshot.isSectionEmpty(0)).thenReturn(true);
        when(snapshot.getBlockTypeId(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR.getId());
        when(snapshot.getBlockTypeId(1, 16, 2)).thenReturn(Material.DIAMOND_BLOCK.getId());

        Map<Material, Integer> counts = SnapshotBlockScanner.countTrackedBlocks(
                snapshot, 0, 17, Set.of(Material.DIAMOND_BLOCK));

        assertThat(counts).containsEntry(Material.DIAMOND_BLOCK, 1).hasSize(1);
        verify(snapshot, never()).getBlockTypeId(0, 0, 0);
    }

    @Test
    @DisplayName("Should count tracked materials from a legacy chunk snapshot")
    void shouldCountTrackedMaterials() {
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);
        when(snapshot.getBlockTypeId(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR.getId());
        when(snapshot.getBlockTypeId(1, 0, 2)).thenReturn(Material.DIAMOND_BLOCK.getId());
        when(snapshot.getBlockTypeId(1, 1, 2)).thenReturn(Material.DIAMOND_BLOCK.getId());

        Map<Material, Integer> counts = SnapshotBlockScanner.countTrackedBlocks(
                snapshot, 0, 2, Set.of(Material.DIAMOND_BLOCK));

        assertThat(counts).containsEntry(Material.DIAMOND_BLOCK, 2).hasSize(1);
    }

    @Test
    @DisplayName("Should use snapshot-relative section indexes below Y zero")
    void shouldUseSnapshotRelativeSectionIndexesBelowZero() {
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);
        when(snapshot.isSectionEmpty(0)).thenReturn(true);
        when(snapshot.getBlockTypeId(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR.getId());
        when(snapshot.getBlockTypeId(1, -48, 2)).thenReturn(Material.DIAMOND_BLOCK.getId());

        Map<Material, Integer> counts = SnapshotBlockScanner.countTrackedBlocks(
                snapshot, -64, -47, Set.of(Material.DIAMOND_BLOCK));

        assertThat(counts).containsEntry(Material.DIAMOND_BLOCK, 1).hasSize(1);
        verify(snapshot).isSectionEmpty(0);
        verify(snapshot).isSectionEmpty(1);
    }

    @Test
    @DisplayName("Should bound pending scans and run only one worker")
    void shouldBoundPendingScansAndWorkers() {
        Plugin plugin = mock(Plugin.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager manager = new CounterDataManager();
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        World world = world();
        when(config.getTrackedBlockMaterials()).thenReturn(Set.of(Material.DIAMOND_BLOCK));
        when(scheduler.runTaskAsynchronously(eq(plugin), any(Runnable.class))).thenReturn(task);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            SnapshotBlockScanner scanner = new SnapshotBlockScanner(plugin, config, manager, 2);

            scanner.scanChunk(chunk(world, 0, 0), ChunkCoord.from(world, 0, 0), true);
            scanner.scanChunk(chunk(world, 1, 0), ChunkCoord.from(world, 1, 0), true);
            scanner.scanChunk(chunk(world, 2, 0), ChunkCoord.from(world, 2, 0), true);
            scanner.scanChunk(chunk(world, 3, 0), ChunkCoord.from(world, 3, 0), true);

            assertThat(scanner.isWorkerRunning()).isTrue();
            assertThat(scanner.getPendingScanCount()).isEqualTo(2);
            verify(scheduler, times(1)).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        }
    }

    @Test
    @DisplayName("Should reject a completed scan after chunk unload")
    void shouldRejectScanAfterUnload() {
        Plugin plugin = mock(Plugin.class);
        PluginConfig config = mock(PluginConfig.class);
        CounterDataManager manager = new CounterDataManager();
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        AtomicReference<Runnable> asyncWork = new AtomicReference<>();
        World world = world();
        Chunk chunk = chunk(world, 0, 0);
        ChunkCoord coord = ChunkCoord.from(world, 0, 0);
        when(config.getTrackedBlockMaterials()).thenReturn(Set.of(Material.DIAMOND_BLOCK));
        doAnswer(invocation -> {
            asyncWork.set(invocation.getArgument(1));
            return task;
        }).when(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return task;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            SnapshotBlockScanner scanner = new SnapshotBlockScanner(plugin, config, manager, 2);
            scanner.scanChunk(chunk, coord, true);
            scanner.cancelScan(coord);
            manager.removeCounterData(coord);

            asyncWork.get().run();

            assertThat(manager.getCounterDataIfPresent(coord)).isNull();
            assertThat(scanner.isWorkerRunning()).isFalse();
        }
    }

    private World world() {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(world.getMaxHeight()).thenReturn(2);
        return world;
    }

    private Chunk chunk(World world, int x, int z) {
        Chunk chunk = mock(Chunk.class);
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);
        when(snapshot.getBlockTypeId(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR.getId());
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(x);
        when(chunk.getZ()).thenReturn(z);
        when(chunk.isLoaded()).thenReturn(true);
        when(chunk.getChunkSnapshot(false, false, false)).thenReturn(snapshot);
        return chunk;
    }
}
