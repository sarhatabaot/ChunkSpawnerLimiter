package com.github.sarhatabaot.chunkspawnerlimiter.watchwolf;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class WatchWolfControlPlugin extends JavaPlugin {
    private static final String STRESS_METADATA = "csl-watchwolf-stress";
    private String extremeStatus = "CSL extreme idle";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length == 0) {
            return false;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin("ChunkSpawnerLimiter");
        if (target == null) {
            sender.sendMessage("CSL test control failed: ChunkSpawnerLimiter is not loaded");
            return true;
        }

        switch (arguments[0].toLowerCase()) {
            case "reload" -> reloadTarget(sender);
            case "set" -> {
                if (arguments.length < 3) {
                    return false;
                }
                target.getConfig().set(arguments[1], parseScalar(arguments[2]));
                target.saveConfig();
                reloadTarget(sender);
            }
            case "set-list" -> {
                if (arguments.length < 2) {
                    return false;
                }
                List<String> values = arguments.length == 2 || arguments[2].isBlank()
                        ? List.of()
                        : Arrays.asList(arguments[2].split(","));
                target.getConfig().set(arguments[1], values);
                target.saveConfig();
                reloadTarget(sender);
            }
            case "stress" -> {
                if (arguments.length != 4) {
                    return false;
                }
                runStressTest(sender, target,
                        Integer.parseInt(arguments[1]),
                        Integer.parseInt(arguments[2]),
                        Integer.parseInt(arguments[3]));
            }
            case "extreme" -> {
                if (arguments.length != 5) {
                    return false;
                }
                startExtremeTest(sender, target, arguments[1],
                        Integer.parseInt(arguments[2]),
                        Integer.parseInt(arguments[3]),
                        Integer.parseInt(arguments[4]));
            }
            case "extreme-status" -> sender.sendMessage(extremeStatus);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Object parseScalar(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static void reloadTarget(CommandSender sender) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
        sender.sendMessage("CSL test control applied");
    }

    private void runStressTest(CommandSender sender, Plugin target, int groupCount,
                               int chunkCount, int entitiesPerChunk) {
        if (groupCount < 1 || chunkCount < 1 || entitiesPerChunk < 1) {
            sender.sendMessage("CSL stress failed: all arguments must be positive");
            return;
        }

        configureStressLimits(target, groupCount);

        long configStarted = System.nanoTime();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
        long configMillis = nanosToMillis(System.nanoTime() - configStarted);

        World world = Bukkit.getWorlds().getFirst();
        List<Chunk> loadedChunks = new ArrayList<>(chunkCount);
        int gridWidth = (int) Math.ceil(Math.sqrt(chunkCount));
        for (int index = 0; index < chunkCount; index++) {
            int chunkX = 100 + index % gridWidth;
            int chunkZ = 100 + index / gridWidth;
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            chunk.load();
            loadedChunks.add(chunk);
            int blockX = chunkX * 16 + 8;
            int blockZ = chunkZ * 16 + 8;
            double blockY = world.getHighestBlockYAt(blockX, blockZ) + 1.0;
            for (int entityIndex = 0; entityIndex < entitiesPerChunk; entityIndex++) {
                Location location = new Location(world,
                        blockX + (entityIndex % 4) * 0.25,
                        blockY,
                        blockZ + (entityIndex / 4) * 0.25);
                ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
                armorStand.setGravity(false);
                armorStand.setMetadata(STRESS_METADATA, new org.bukkit.metadata.FixedMetadataValue(this, true));
            }
        }

        long populatedStarted = System.nanoTime();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
        long populatedMillis = nanosToMillis(System.nanoTime() - populatedStarted);
        long retainedEntities = world.getEntities().stream()
                .filter(entity -> entity.hasMetadata(STRESS_METADATA))
                .count();

        sender.sendMessage("CSL stress complete groups=%d chunks=%d entities=%d config-ms=%d populated-ms=%d"
                .formatted(groupCount, loadedChunks.size(), retainedEntities, configMillis, populatedMillis));
    }

    private void startExtremeTest(CommandSender sender, Plugin target, String scenario,
                                  int chunkCount, int entitiesPerChunk, int chunksPerTick) {
        if (extremeStatus.startsWith("CSL extreme running")) {
            sender.sendMessage("CSL extreme failed: another workload is running");
            return;
        }
        if (chunkCount < 1 || entitiesPerChunk < 0 || chunksPerTick < 1
                || (!"chunks".equals(scenario) && !"entities".equals(scenario))) {
            sender.sendMessage("CSL extreme failed: invalid arguments");
            return;
        }

        extremeStatus = "CSL extreme running scenario=%s chunks=%d entities-per-chunk=%d"
                .formatted(scenario, chunkCount, entitiesPerChunk);
        sender.sendMessage(extremeStatus);
        configureStressLimits(target, 300);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
        new ExtremeStressTask(scenario, chunkCount, entitiesPerChunk, chunksPerTick)
                .runTaskTimer(this, 1L, 1L);
    }

    private static void configureStressLimits(Plugin target, int groupCount) {
        List<String> entityMembers = List.of(EntityType.ARMOR_STAND.name());
        List<String> blockMembers = List.of("WHITE_WOOL");
        target.getConfig().set("entities.limits.TEST_DECORATIONS", 100_000);
        for (int index = 0; index < groupCount; index++) {
            String suffix = String.format("%04d", index);
            target.getConfig().set("entities.entity-groups.STRESS_ENTITY_" + suffix, entityMembers);
            target.getConfig().set("entities.limits.STRESS_ENTITY_" + suffix, 100_000);
            target.getConfig().set("blocks.block-groups.STRESS_BLOCK_" + suffix, blockMembers);
            target.getConfig().set("blocks.limits.STRESS_BLOCK_" + suffix, 100_000);
        }
        target.saveConfig();
    }

    private final class ExtremeStressTask extends BukkitRunnable {
        private final String scenario;
        private final int chunkCount;
        private final int entitiesPerChunk;
        private final int chunksPerTick;
        private final World world;
        private final long startedNanos = System.nanoTime();
        private final long heapBeforeMb = usedHeapMb();
        private long peakHeapMb = heapBeforeMb;
        private int chunkIndex;
        private int createdEntities;

        private ExtremeStressTask(String scenario, int chunkCount,
                                  int entitiesPerChunk, int chunksPerTick) {
            this.scenario = scenario;
            this.chunkCount = chunkCount;
            this.entitiesPerChunk = entitiesPerChunk;
            this.chunksPerTick = chunksPerTick;
            this.world = Bukkit.getWorlds().getFirst();
        }

        @Override
        public void run() {
            try {
                int end = Math.min(chunkCount, chunkIndex + chunksPerTick);
                while (chunkIndex < end) {
                    populateChunk(chunkIndex++);
                }
                peakHeapMb = Math.max(peakHeapMb, usedHeapMb());
                if (chunkIndex == chunkCount) {
                    finishReport();
                    cancel();
                }
            } catch (RuntimeException exception) {
                extremeStatus = "CSL extreme failed scenario=" + scenario + " error="
                        + exception.getClass().getSimpleName();
                getLogger().log(java.util.logging.Level.SEVERE, extremeStatus, exception);
                cancel();
            }
        }

        private void populateChunk(int index) {
            int gridWidth = (int) Math.ceil(Math.sqrt(chunkCount));
            int chunkX = 200 + index % gridWidth;
            int chunkZ = 200 + index / gridWidth;
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            chunk.load();
            forceLoad(chunk);

            int blockX = chunkX * 16 + 8;
            int blockZ = chunkZ * 16 + 8;
            double blockY = world.getHighestBlockYAt(blockX, blockZ) + 1.0;
            for (int entityIndex = 0; entityIndex < entitiesPerChunk; entityIndex++) {
                Location location = new Location(world,
                        blockX + (entityIndex % 4) * 0.25,
                        blockY,
                        blockZ + (entityIndex / 4) * 0.25);
                ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
                armorStand.setGravity(false);
                armorStand.setMetadata(STRESS_METADATA,
                        new org.bukkit.metadata.FixedMetadataValue(WatchWolfControlPlugin.this, true));
                createdEntities++;
            }
        }

        private void finishReport() {
            long reloadStarted = System.nanoTime();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
            long reloadMillis = nanosToMillis(System.nanoTime() - reloadStarted);
            long retainedEntities = world.getEntities().stream()
                    .filter(entity -> entity.hasMetadata(STRESS_METADATA))
                    .count();
            long heapAfterMb = usedHeapMb();
            peakHeapMb = Math.max(peakHeapMb, heapAfterMb);

            extremeStatus = String.format(Locale.ROOT,
                    "CSL EXTREME REPORT scenario=%s workload-chunks=%d loaded-chunks=%d "
                            + "created-entities=%d retained-entities=%d duration-ms=%d reload-ms=%d "
                            + "heap-before-mb=%d heap-after-mb=%d heap-peak-mb=%d heap-max-mb=%d "
                            + "tps-1m=%.2f mspt=%.2f",
                    scenario, chunkCount, world.getLoadedChunks().length,
                    createdEntities, retainedEntities,
                    nanosToMillis(System.nanoTime() - startedNanos), reloadMillis,
                    heapBeforeMb, heapAfterMb, peakHeapMb,
                    Runtime.getRuntime().maxMemory() / 1_048_576L,
                    oneMinuteTps(), averageTickMillis());
            getLogger().info(extremeStatus);
        }
    }

    private static void forceLoad(Chunk chunk) {
        try {
            chunk.getClass().getMethod("setForceLoaded", boolean.class).invoke(chunk, true);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to retain stress-test chunk", exception);
        }
    }

    private static long usedHeapMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L;
    }

    private static double oneMinuteTps() {
        try {
            double[] ticksPerSecond = (double[]) Bukkit.class.getMethod("getTPS").invoke(null);
            return ticksPerSecond[0];
        } catch (ReflectiveOperationException exception) {
            return -1.0;
        }
    }

    private static double averageTickMillis() {
        try {
            return ((Number) Bukkit.class.getMethod("getAverageTickTime").invoke(null)).doubleValue();
        } catch (ReflectiveOperationException exception) {
            return -1.0;
        }
    }

    private static long nanosToMillis(long nanos) {
        return Math.max(1, Math.round(nanos / 1_000_000.0));
    }
}
