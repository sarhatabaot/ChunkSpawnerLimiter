package com.github.sarhatabaot.chunkspawnerlimiter.counter;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for CounterData.
 * Tests thread-safe counting operations for blocks and entities.
 */
@DisplayName("CounterData Tests")
class CounterDataTest {

    private CounterData counterData;

    @BeforeEach
    void setUp() {
        counterData = new CounterData();
    }

    @Test
    @DisplayName("Should initialize with zero counts")
    void shouldInitializeWithZeroCounts() {
        assertThat(counterData.getBlockCount(Material.STONE)).isZero();
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isZero();
    }

    @Test
    @DisplayName("Should increment block counts correctly")
    void shouldIncrementBlockCountsCorrectly() {
        // When
        counterData.incrementBlock(Material.STONE);
        counterData.incrementBlock(Material.STONE);
        counterData.incrementBlock(Material.DIRT);

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isEqualTo(2);
        assertThat(counterData.getBlockCount(Material.DIRT)).isEqualTo(1);
        assertThat(counterData.getBlockCount(Material.SAND)).isZero();
    }

    @Test
    @DisplayName("Should increment entity counts correctly")
    void shouldIncrementEntityCountsCorrectly() {
        // When
        counterData.incrementEntity(EntityType.ZOMBIE);
        counterData.incrementEntity(EntityType.ZOMBIE);
        counterData.incrementEntity(EntityType.SKELETON);

        // Then
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isEqualTo(2);
        assertThat(counterData.getEntityCount(EntityType.SKELETON)).isEqualTo(1);
        assertThat(counterData.getEntityCount(EntityType.CREEPER)).isZero();
    }

    @Test
    @DisplayName("Should decrement block counts correctly")
    void shouldDecrementBlockCountsCorrectly() {
        // Given
        counterData.setBlockCount(Material.STONE, 5);

        // When
        counterData.decrementBlock(Material.STONE);
        counterData.decrementBlock(Material.STONE);

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isEqualTo(3);
    }

    @Test
    @DisplayName("Should not decrement below zero for blocks")
    void shouldNotDecrementBelowZeroForBlocks() {
        // Given
        counterData.setBlockCount(Material.STONE, 1);

        // When
        counterData.decrementBlock(Material.STONE);
        counterData.decrementBlock(Material.STONE); // Should not go below 0

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isZero();
    }

    @Test
    @DisplayName("Should not decrement below zero for entities")
    void shouldNotDecrementBelowZeroForEntities() {
        // Given
        counterData.setEntityCount(EntityType.ZOMBIE, 1);

        // When
        counterData.decrementEntity(EntityType.ZOMBIE);
        counterData.decrementEntity(EntityType.ZOMBIE); // Should not go below 0

        // Then
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isZero();
    }

    @Test
    @DisplayName("Should set block count correctly")
    void shouldSetBlockCountCorrectly() {
        // When
        counterData.setBlockCount(Material.STONE, 10);

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isEqualTo(10);
    }

    @Test
    @DisplayName("Should set entity count correctly")
    void shouldSetEntityCountCorrectly() {
        // When
        counterData.setEntityCount(EntityType.ZOMBIE, 15);

        // Then
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isEqualTo(15);
    }

    @Test
    @DisplayName("Should throw exception for negative block count")
    void shouldThrowExceptionForNegativeBlockCount() {
        assertThatThrownBy(() -> counterData.setBlockCount(Material.STONE, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Count cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception for negative entity count")
    void shouldThrowExceptionForNegativeEntityCount() {
        assertThatThrownBy(() -> counterData.setEntityCount(EntityType.ZOMBIE, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Count cannot be negative");
    }

    @Test
    @DisplayName("Should return tracked block types correctly")
    void shouldReturnTrackedBlockTypesCorrectly() {
        // Given
        counterData.incrementBlock(Material.STONE);
        counterData.incrementBlock(Material.DIRT);
        counterData.setBlockCount(Material.SAND, 5);

        // When
        Set<Material> trackedTypes = counterData.getTrackedBlockTypes();

        // Then
        assertThat(trackedTypes)
            .hasSize(3)
            .containsExactlyInAnyOrder(Material.STONE, Material.DIRT, Material.SAND);
    }

    @Test
    @DisplayName("Should return tracked entity types correctly")
    void shouldReturnTrackedEntityTypesCorrectly() {
        // Given
        counterData.incrementEntity(EntityType.ZOMBIE);
        counterData.incrementEntity(EntityType.SKELETON);
        counterData.setEntityCount(EntityType.CREEPER, 3);

        // When
        Set<EntityType> trackedTypes = counterData.getTrackedEntityTypes();

        // Then
        assertThat(trackedTypes)
            .hasSize(3)
            .containsExactlyInAnyOrder(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER);
    }

    @Test
    @DisplayName("Should handle concurrent block increments correctly")
    void shouldHandleConcurrentBlockIncrementsCorrectly() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counterData.incrementBlock(Material.STONE);
                }
                latch.countDown();
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Then
        assertThat(counterData.getBlockCount(Material.STONE))
            .isEqualTo(numberOfThreads * incrementsPerThread);
    }

    @Test
    @DisplayName("Should handle concurrent entity increments correctly")
    void shouldHandleConcurrentEntityIncrementsCorrectly() throws InterruptedException {
        // Given
        int numberOfThreads = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counterData.incrementEntity(EntityType.ZOMBIE);
                }
                latch.countDown();
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Then
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE))
            .isEqualTo(numberOfThreads * incrementsPerThread);
    }

    @Test
    @DisplayName("Should handle concurrent mixed operations correctly")
    void shouldHandleConcurrentMixedOperationsCorrectly() throws InterruptedException {
        // Given
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // When - Multiple threads performing different operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Thread 0: Increment blocks
                    if (threadId == 0) {
                        for (int j = 0; j < 50; j++) {
                            counterData.incrementBlock(Material.STONE);
                        }
                    }
                    // Thread 1: Increment entities
                    else if (threadId == 1) {
                        for (int j = 0; j < 50; j++) {
                            counterData.incrementEntity(EntityType.ZOMBIE);
                        }
                    }
                    // Thread 2: Decrement blocks
                    else if (threadId == 2) {
                        counterData.setBlockCount(Material.STONE, 100);
                        for (int j = 0; j < 30; j++) {
                            counterData.decrementBlock(Material.STONE);
                        }
                    }
                    // Thread 3: Decrement entities
                    else if (threadId == 3) {
                        counterData.setEntityCount(EntityType.ZOMBIE, 100);
                        for (int j = 0; j < 30; j++) {
                            counterData.decrementEntity(EntityType.ZOMBIE);
                        }
                    }
                    // Thread 4: Set operations
                    else if (threadId == 4) {
                        counterData.setBlockCount(Material.DIAMOND_BLOCK, 25);
                        counterData.setEntityCount(EntityType.SKELETON, 25);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isEqualTo(100 - 30 + 50); // 120
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isEqualTo(100 - 30 + 50); // 120
        assertThat(counterData.getBlockCount(Material.DIAMOND_BLOCK)).isEqualTo(25);
        assertThat(counterData.getEntityCount(EntityType.SKELETON)).isEqualTo(25);
    }

    @Test
    @DisplayName("Should handle zero decrement operations gracefully")
    void shouldHandleZeroDecrementOperationsGracefully() {
        // When - Attempting to decrement non-existent counters
        counterData.decrementBlock(Material.STONE);
        counterData.decrementEntity(EntityType.ZOMBIE);

        // Then - Should not create negative counts
        assertThat(counterData.getBlockCount(Material.STONE)).isZero();
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isZero();
    }

    @Test
    @DisplayName("Should maintain separate counts for blocks and entities")
    void shouldMaintainSeparateCountsForBlocksAndEntities() {
        // When
        counterData.incrementBlock(Material.STONE);
        counterData.incrementEntity(EntityType.ZOMBIE);

        // Then
        assertThat(counterData.getBlockCount(Material.STONE)).isEqualTo(1);
        assertThat(counterData.getEntityCount(EntityType.ZOMBIE)).isEqualTo(1);

        // And they shouldn't interfere with each other
        assertThat(counterData.getBlockCount(Material.STONE)).isNotEqualTo(
            counterData.getEntityCount(EntityType.ZOMBIE));
    }
}
