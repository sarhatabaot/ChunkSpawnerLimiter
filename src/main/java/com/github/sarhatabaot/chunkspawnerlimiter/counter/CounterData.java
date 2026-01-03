package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores and manages block and entity counts for a chunk.
 * <p>
 * This class is thread-safe and designed to be used in concurrent environments
 * such as asynchronous chunk scanning or entity tracking.
 * <p>
 * Counts are never allowed to become negative. Decrements are clamped at zero.
 */
@ThreadSafe
public class CounterData {

    /**
     * Map holding counts for tracked block types.
     */
    private final Map<Material, AtomicInteger> blockCounts = new ConcurrentHashMap<>();

    /**
     * Map holding counts for tracked entity types.
     */
    private final Map<EntityType, AtomicInteger> entityCounts = new ConcurrentHashMap<>();

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    /**
     * Safely increments the counter associated with the given key.
     * <p>
     * If the key does not yet exist, it will be initialized with a count of {@code 0}
     * before being incremented.
     *
     * @param map the map containing counters
     * @param key the key whose counter should be incremented
     */
    private static <K> void safeIncrement(Map<K, AtomicInteger> map, K key) {
        map.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Safely decrements the counter associated with the given key.
     * <p>
     * If the counter reaches zero, it will not go below zero.
     * If the key does not exist, this method does nothing.
     *
     * @param map the map containing counters
     * @param key the key whose counter should be decremented
     */
    private static <K> void safeDecrement(Map<K, AtomicInteger> map, K key) {
        AtomicInteger count = map.get(key);
        if (count != null) {
            count.updateAndGet(c -> Math.max(0, c - 1));
        }
    }

    /**
     * Sets the counter value for the given key.
     *
     * @param map   the map containing counters
     * @param key   the key whose counter should be set
     * @param value the new counter value (must be {@code >= 0})
     *
     * @throws IllegalArgumentException if {@code value} is negative
     */
    private static <K> void safeSet(Map<K, AtomicInteger> map, K key, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        map.put(key, new AtomicInteger(value));
    }

    // ---------------------------------------------------------------------
    // Blocks
    // ---------------------------------------------------------------------

    /**
     * Increments the count for the given block type.
     *
     * @param type the block type to increment
     */
    public void incrementBlock(Material type) {
        safeIncrement(blockCounts, type);
    }

    /**
     * Decrements the count for the given block type.
     * <p>
     * The count will not go below zero.
     *
     * @param type the block type to decrement
     */
    public void decrementBlock(Material type) {
        safeDecrement(blockCounts, type);
    }

    /**
     * Sets the count for the given block type.
     *
     * @param type  the block type
     * @param count the new count (must be {@code >= 0})
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public void setBlockCount(Material type, int count) {
        safeSet(blockCounts, type, count);
    }

    /**
     * Returns the current count for the given block type.
     *
     * @param type the block type
     * @return the current count, or {@code 0} if the block type is not tracked
     */
    public int getBlockCount(Material type) {
        return blockCounts.getOrDefault(type, new AtomicInteger(0)).get();
    }

    /**
     * Returns an unmodifiable view of all tracked block types.
     *
     * @return a set of tracked block types
     */
    public Set<Material> getTrackedBlockTypes() {
        return Collections.unmodifiableSet(blockCounts.keySet());
    }

    // ---------------------------------------------------------------------
    // Entities
    // ---------------------------------------------------------------------

    /**
     * Increments the count for the given entity type.
     *
     * @param type the entity type to increment
     */
    public void incrementEntity(EntityType type) {
        safeIncrement(entityCounts, type);
    }

    /**
     * Decrements the count for the given entity type.
     * <p>
     * The count will not go below zero.
     *
     * @param type the entity type to decrement
     */
    public void decrementEntity(EntityType type) {
        safeDecrement(entityCounts, type);
    }

    /**
     * Sets the count for the given entity type.
     *
     * @param type  the entity type
     * @param count the new count (must be {@code >= 0})
     *
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public void setEntityCount(EntityType type, int count) {
        safeSet(entityCounts, type, count);
    }

    /**
     * Returns the current count for the given entity type.
     *
     * @param type the entity type
     * @return the current count, or {@code 0} if the entity type is not tracked
     */
    public int getEntityCount(EntityType type) {
        return entityCounts.getOrDefault(type, new AtomicInteger(0)).get();
    }

    /**
     * Returns an unmodifiable view of all tracked entity types.
     *
     * @return a set of tracked entity types
     */
    public Set<EntityType> getTrackedEntityTypes() {
        return Collections.unmodifiableSet(entityCounts.keySet());
    }
}
