package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Map;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CounterData {
    private final Map<Material, AtomicInteger> blockCounts = new ConcurrentHashMap<>();
    private final Map<EntityType, AtomicInteger> entityCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> entityGroupCounts = new ConcurrentHashMap<>();

    public Set<EntityType> getTrackedEntityTypes() {
        return entityCounts.keySet();
    }

    public Set<String> getTrackedEntityGroups() {
        return entityGroupCounts.keySet();
    }

    public int getEntityGroupCount(String group) {
        AtomicInteger count = entityGroupCounts.get(group);
        return count != null ? count.get() : 0;
    }
    public int getBlockCount(Material type) {
        AtomicInteger count = blockCounts.get(type);
        return count != null ? count.get() : 0;
    }

    public int getEntityCount(EntityType type) {
        AtomicInteger count = entityCounts.get(type);
        return count != null ? count.get() : 0;
    }

    public void incrementBlock(Material type) {
        blockCounts.computeIfAbsent(type, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementBlock(Material type) {
        AtomicInteger count = blockCounts.get(type);
        if (count != null) {
            count.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    public void incrementEntity(EntityType type) {
        entityCounts.computeIfAbsent(type, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementEntity(EntityType type) {
        AtomicInteger count = entityCounts.get(type);
        if (count != null) {
            count.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    public void incrementEntityGroup(final String group) {
        entityGroupCounts.computeIfAbsent(group, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementEntityGroup(final String group) {
        AtomicInteger count = entityGroupCounts.get(group);
        if (count != null) {
            count.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    public void setEntityGroupCount(final String group, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        entityGroupCounts.put(group, new AtomicInteger(count));
    }

    public void setBlockCount(Material type, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        blockCounts.put(type, new AtomicInteger(count));
    }

    public void setEntityCount(EntityType type, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative");
        }
        entityCounts.put(type, new AtomicInteger(count));
    }
}