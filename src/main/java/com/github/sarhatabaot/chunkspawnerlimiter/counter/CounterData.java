package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;

public class CounterData {

    private final Map<Material, AtomicInteger> blockCounts = new ConcurrentHashMap<>();
    private final Map<EntityType, AtomicInteger> entityCounts = new ConcurrentHashMap<>();

    // -------------------------
    // Unified increment/decrement
    // -------------------------
    private static void safeIncrement(Map<?, AtomicInteger> map, Object key) {
        map.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private static void safeDecrement(Map<?, AtomicInteger> map, Object key) {
        AtomicInteger count = map.get(key);
        if (count != null) {
            count.updateAndGet(c -> Math.max(0, c - 1));
        }
    }

    private static void safeSet(Map<?, AtomicInteger> map, Object key, int value) {
        if (value < 0) throw new IllegalArgumentException("Count cannot be negative");
        map.put(key, new AtomicInteger(value));
    }

    // -------------------------
    // Blocks
    // -------------------------
    public void incrementBlock(Material type) {
        safeIncrement(blockCounts, type);
    }

    public void decrementBlock(Material type) {
        safeDecrement(blockCounts, type);
    }

    public void setBlockCount(Material type, int count) {
        safeSet(blockCounts, type, count);
    }

    public int getBlockCount(Material type) {
        return blockCounts.getOrDefault(type, new AtomicInteger(0)).get();
    }

    public Set<Material> getTrackedBlockTypes() {
        return Collections.unmodifiableSet(blockCounts.keySet());
    }

    // -------------------------
    // Entities
    // -------------------------
    public void incrementEntity(EntityType type) {
        safeIncrement(entityCounts, type);
    }

    public void decrementEntity(EntityType type) {
        safeDecrement(entityCounts, type);
    }

    public void setEntityCount(EntityType type, int count) {
        safeSet(entityCounts, type, count);
    }

    public int getEntityCount(EntityType type) {
        return entityCounts.getOrDefault(type, new AtomicInteger(0)).get();
    }

    public Set<EntityType> getTrackedEntityTypes() {
        return Collections.unmodifiableSet(entityCounts.keySet());
    }

    // -------------------------
    // Entity group counts (on-demand)
    // -------------------------
    /**
     * Compute current count of all entities in a given group.
     * This avoids maintaining a separate map for groups.
     */
    public int getEntityGroupCount(String group, PluginConfig config) {
        if (group == null) return 0;

        return entityCounts.entrySet().stream()
                .filter(e -> group.equals(config.getEntityGroup(e.getKey())))
                .mapToInt(e -> e.getValue().get())
                .sum();
    }

    /**
     * Convenience: increment group counts (increments underlying EntityType counts)
     * Requires that PluginConfig resolves a representative EntityType for the group.
     */
    public void incrementEntityGroup(String group, PluginConfig config) {
        // Increment each EntityType in the group
        config.getEntityGroups().getOrDefault(group, Collections.emptyList())
                .stream()
                .map(EntityType::valueOf)
                .forEach(this::incrementEntity);
    }

    public void decrementEntityGroup(String group, PluginConfig config) {
        config.getEntityGroups().getOrDefault(group, Collections.emptyList())
                .stream()
                .map(EntityType::valueOf)
                .forEach(this::decrementEntity);
    }
}
