package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import com.github.sarhatabaot.chunkspawnerlimiter.CSLLogger;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.chunk.ChunkCoord;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterData;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.WorldReflection;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.BlockScanner;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SnapshotBlockScanner implements BlockScanner {
    static final int DEFAULT_MAX_PENDING_SCANS = 1024;
    static final int MAX_BACKFILL_CHUNKS_PER_PASS = 32;
    private static final int CHUNK_SIZE = 16;
    private static final Map<Class<?>, SnapshotAccessor> SNAPSHOT_ACCESSORS = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final PluginConfig config;
    private final CounterDataManager counterManager;
    private final int maxPendingScans;
    private final Map<ChunkCoord, ScanRequest> pendingScans = new LinkedHashMap<>();
    private final Map<ChunkCoord, Long> generations = new LinkedHashMap<>();

    private long nextGeneration;
    private volatile boolean workerRunning;
    private boolean needsBackfill;
    private Iterator<World> backfillWorlds;
    private Chunk[] backfillChunks = new Chunk[0];
    private int backfillChunkIndex;
    private volatile boolean stopped;

    public SnapshotBlockScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        this(plugin, config, counterManager, DEFAULT_MAX_PENDING_SCANS);
    }

    SnapshotBlockScanner(Plugin plugin, PluginConfig config, CounterDataManager counterManager,
                         int maxPendingScans) {
        if (maxPendingScans < 1) {
            throw new IllegalArgumentException("maxPendingScans must be positive");
        }
        this.plugin = plugin;
        this.config = config;
        this.counterManager = counterManager;
        this.maxPendingScans = maxPendingScans;
    }

    @Override
    public @Nullable Material getMaterialAt(World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType();
    }

    @Override
    public void scanChunk(Chunk chunk, ChunkCoord coord, boolean async) {
        Set<Material> trackedMaterials = Set.copyOf(config.getTrackedBlockMaterials());
        if (trackedMaterials.isEmpty() || stopped) {
            return;
        }

        if (!async) {
            scanImmediately(chunk, coord, trackedMaterials);
            return;
        }

        enqueue(chunk, coord);
        startNextScan();
    }

    @Override
    public void cancelScan(ChunkCoord coord) {
        pendingScans.remove(coord);
        generations.remove(coord);
    }

    @Override
    public void shutdown() {
        stopped = true;
        pendingScans.clear();
        generations.clear();
        needsBackfill = false;
        resetBackfillCursor();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String getImplementationName() {
        return "BoundedSnapshot";
    }

    int getPendingScanCount() {
        return pendingScans.size();
    }

    boolean isWorkerRunning() {
        return workerRunning;
    }

    boolean hasGeneration(ChunkCoord coord) {
        return generations.containsKey(coord);
    }

    private void scanImmediately(Chunk chunk, ChunkCoord coord, Set<Material> trackedMaterials) {
        CounterData data = counterManager.getCounterData(coord);
        long revision = data.getBlockRevision();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
        Map<Material, Integer> counts = countTrackedBlocks(
                snapshot,
                WorldReflection.getWorldMinHeightSafe(chunk.getWorld()),
                chunk.getWorld().getMaxHeight(),
                trackedMaterials
        );
        data.replaceBlockCounts(revision, counts);
    }

    private void enqueue(Chunk chunk, ChunkCoord coord) {
        if (!pendingScans.containsKey(coord) && pendingScans.size() >= maxPendingScans) {
            needsBackfill = true;
            return;
        }

        long generation = ++nextGeneration;
        generations.put(coord, generation);
        pendingScans.put(coord, new ScanRequest(chunk, coord, generation));
    }

    private void startNextScan() {
        if (workerRunning || stopped) {
            return;
        }

        if (pendingScans.isEmpty() && needsBackfill) {
            backfillLoadedChunks();
        }

        ScanRequest request = pollNextRequest();
        while (request != null && !request.chunk().isLoaded()) {
            generations.remove(request.coord(), request.generation());
            request = pollNextRequest();
        }
        if (request == null) {
            return;
        }

        Set<Material> trackedMaterials = Set.copyOf(config.getTrackedBlockMaterials());
        if (trackedMaterials.isEmpty()) {
            generations.remove(request.coord(), request.generation());
            startNextScan();
            return;
        }

        CounterData data = counterManager.getCounterData(request.coord());
        long blockRevision = data.getBlockRevision();
        ChunkSnapshot snapshot;
        int minY;
        int maxY;
        try {
            snapshot = request.chunk().getChunkSnapshot(false, false, false);
            minY = WorldReflection.getWorldMinHeightSafe(request.chunk().getWorld());
            maxY = request.chunk().getWorld().getMaxHeight();
        } catch (RuntimeException exception) {
            releaseForRetry(request);
            plugin.getLogger().warning("Unable to capture chunk snapshot for " + request.coord() + ": "
                    + exception.getMessage());
            scheduleRetry();
            return;
        }

        workerRunning = true;
        ScanRequest activeRequest = request;
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Map<Material, Integer> counts;
                try {
                    counts = countTrackedBlocks(snapshot, minY, maxY, trackedMaterials);
                } catch (RuntimeException exception) {
                    scheduleCompletion(activeRequest, blockRevision, null, exception);
                    return;
                }
                scheduleCompletion(activeRequest, blockRevision, counts, null);
            });
        } catch (RuntimeException exception) {
            workerRunning = false;
            releaseForRetry(request);
            plugin.getLogger().warning("Unable to schedule chunk snapshot scan: " + exception.getMessage());
            scheduleRetry();
        }
    }

    private void scheduleCompletion(ScanRequest request, long blockRevision,
                                    @Nullable Map<Material, Integer> counts,
                                    @Nullable RuntimeException failure) {
        if (stopped) {
            return;
        }

        try {
            Bukkit.getScheduler().runTask(plugin,
                    () -> completeScan(request, blockRevision, counts, failure));
        } catch (RuntimeException exception) {
            workerRunning = false;
            releaseForRetry(request);
            if (!stopped) {
                plugin.getLogger().warning("Unable to apply chunk snapshot scan: " + exception.getMessage());
            }
        }
    }

    private void completeScan(ScanRequest request, long blockRevision,
                              @Nullable Map<Material, Integer> counts,
                              @Nullable RuntimeException failure) {
        workerRunning = false;
        if (stopped) {
            return;
        }

        Long currentGeneration = generations.get(request.coord());
        if (currentGeneration == null || currentGeneration != request.generation()
                || !request.chunk().isLoaded()) {
            generations.remove(request.coord(), request.generation());
            startNextScan();
            return;
        }

        if (failure != null || counts == null) {
            releaseForRetry(request);
            plugin.getLogger().warning("Unable to scan chunk snapshot for " + request.coord() + ": "
                    + (failure == null ? "unknown error" : failure.getMessage()));
            scheduleRetry();
            return;
        }

        CounterData data = counterManager.getCounterDataIfPresent(request.coord());
        if (data != null && !data.replaceBlockCounts(blockRevision, counts)) {
            generations.remove(request.coord(), request.generation());
            enqueue(request.chunk(), request.coord());
        }

        CSLLogger.debug(() -> "[BoundedSnapshot] Scanned chunk " + request.coord()
                + " with " + counts.values().stream().mapToInt(Integer::intValue).sum()
                + " tracked blocks");
        startNextScan();
    }

    private ScanRequest pollNextRequest() {
        Iterator<Map.Entry<ChunkCoord, ScanRequest>> iterator = pendingScans.entrySet().iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        ScanRequest request = iterator.next().getValue();
        iterator.remove();
        return request;
    }

    private void releaseForRetry(ScanRequest request) {
        if (generations.remove(request.coord(), request.generation())) {
            needsBackfill = true;
        }
    }

    private void scheduleRetry() {
        if (stopped) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, this::startNextScan);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Unable to schedule chunk snapshot retry: " + exception.getMessage());
        }
    }

    private void backfillLoadedChunks() {
        if (backfillWorlds == null) {
            backfillWorlds = new ArrayList<>(Bukkit.getWorlds()).iterator();
        }

        int visited = 0;
        while (visited < MAX_BACKFILL_CHUNKS_PER_PASS && pendingScans.size() < maxPendingScans) {
            if (backfillChunkIndex >= backfillChunks.length) {
                if (!loadNextBackfillWorld()) {
                    needsBackfill = false;
                    resetBackfillCursor();
                    return;
                }
            }

            Chunk chunk = backfillChunks[backfillChunkIndex++];
            visited++;
            ChunkCoord coord = ChunkCoord.from(chunk);
            if (!generations.containsKey(coord)) {
                enqueue(chunk, coord);
            }
        }
        needsBackfill = true;
    }

    private boolean loadNextBackfillWorld() {
        while (backfillWorlds.hasNext()) {
            World world = backfillWorlds.next();
            if (!config.isWorldDisabled(world.getName())) {
                backfillChunks = world.getLoadedChunks();
                backfillChunkIndex = 0;
                if (backfillChunks.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void resetBackfillCursor() {
        backfillWorlds = null;
        backfillChunks = new Chunk[0];
        backfillChunkIndex = 0;
    }

    static Map<Material, Integer> countTrackedBlocks(ChunkSnapshot snapshot, int minY, int maxY,
                                                     Set<Material> trackedMaterials) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        SnapshotAccessor accessor = SNAPSHOT_ACCESSORS.computeIfAbsent(
                snapshot.getClass(), SnapshotBlockScanner::createSnapshotAccessor);
        int firstSection = Math.floorDiv(minY, CHUNK_SIZE);
        int lastSection = Math.floorDiv(maxY - 1, CHUNK_SIZE);
        for (int sectionY = firstSection; sectionY <= lastSection; sectionY++) {
            if (accessor.isSectionEmpty(snapshot, sectionY - firstSection)) {
                continue;
            }
            int sectionMinY = Math.max(minY, sectionY * CHUNK_SIZE);
            int sectionMaxY = Math.min(maxY, sectionMinY + CHUNK_SIZE);
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    for (int y = sectionMinY; y < sectionMaxY; y++) {
                        Material material = accessor.getMaterial(snapshot, x, y, z);
                        if (trackedMaterials.contains(material)) {
                            counts.merge(material, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return counts;
    }

    private static SnapshotAccessor createSnapshotAccessor(Class<?> snapshotClass) {
        SnapshotSectionAccessor sectionAccessor = createSectionAccessor(snapshotClass);
        try {
            Method getBlockType = snapshotClass.getMethod(
                    "getBlockType", int.class, int.class, int.class);
            MethodHandle handle = MethodHandles.publicLookup().unreflect(getBlockType).asType(
                    MethodType.methodType(Material.class, ChunkSnapshot.class,
                            int.class, int.class, int.class));
            return new SnapshotAccessor(
                    (snapshot, x, y, z) -> invokeMaterial(handle, snapshot, x, y, z),
                    sectionAccessor);
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to access chunk snapshot", exception);
        }

        try {
            Method getBlockTypeId = snapshotClass.getMethod(
                    "getBlockTypeId", int.class, int.class, int.class);
            MethodHandle handle = MethodHandles.publicLookup().unreflect(getBlockTypeId).asType(
                    MethodType.methodType(int.class, ChunkSnapshot.class,
                            int.class, int.class, int.class));
            return new SnapshotAccessor(
                    (snapshot, x, y, z) -> Material.getMaterial(
                            invokeMaterialId(handle, snapshot, x, y, z)),
                    sectionAccessor);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unsupported ChunkSnapshot implementation: "
                    + snapshotClass.getName(), exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to access chunk snapshot", exception);
        }
    }

    private static SnapshotSectionAccessor createSectionAccessor(Class<?> snapshotClass) {
        try {
            Method method = snapshotClass.getMethod("isSectionEmpty", int.class);
            MethodHandle handle = MethodHandles.publicLookup().unreflect(method).asType(
                    MethodType.methodType(boolean.class, ChunkSnapshot.class, int.class));
            return (snapshot, sectionY) -> invokeSectionEmpty(handle, snapshot, sectionY);
        } catch (NoSuchMethodException ignored) {
            return (snapshot, sectionY) -> false;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to access chunk snapshot sections", exception);
        }
    }

    private static Material invokeMaterial(MethodHandle handle, ChunkSnapshot snapshot, int x, int y, int z) {
        try {
            return (Material) handle.invokeExact(snapshot, x, y, z);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to read chunk snapshot", exception);
        }
    }

    private static int invokeMaterialId(MethodHandle handle, ChunkSnapshot snapshot, int x, int y, int z) {
        try {
            return (int) handle.invokeExact(snapshot, x, y, z);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to read chunk snapshot", exception);
        }
    }

    private static boolean invokeSectionEmpty(MethodHandle handle, ChunkSnapshot snapshot, int sectionY) {
        try {
            return (boolean) handle.invokeExact(snapshot, sectionY);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to read chunk snapshot section", exception);
        }
    }

    @FunctionalInterface
    private interface SnapshotMaterialAccessor {
        Material get(ChunkSnapshot snapshot, int x, int y, int z);
    }

    @FunctionalInterface
    private interface SnapshotSectionAccessor {
        boolean isEmpty(ChunkSnapshot snapshot, int sectionY);
    }

    private record SnapshotAccessor(SnapshotMaterialAccessor materialAccessor,
                                    SnapshotSectionAccessor sectionAccessor) {
        private Material getMaterial(ChunkSnapshot snapshot, int x, int y, int z) {
            return materialAccessor.get(snapshot, x, y, z);
        }

        private boolean isSectionEmpty(ChunkSnapshot snapshot, int sectionY) {
            return sectionAccessor.isEmpty(snapshot, sectionY);
        }
    }

    private record ScanRequest(Chunk chunk, ChunkCoord coord, long generation) {
    }
}
