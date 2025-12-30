package com.github.sarhatabaot.chunkspawnerlimiter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for the main ChunkSpawnerLimiter plugin class.
 * More comprehensive tests are in the integration package.
 */
@DisplayName("ChunkSpawnerLimiter Main Class Tests")
class ChunkSpawnerLimiterTest {

    private ChunkSpawnerLimiter plugin;

    @BeforeEach
    void setUp() {
        // Load plugin with MockBukkit
        plugin = MockBukkit.load(ChunkSpawnerLimiter.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should create plugin instance")
    void shouldCreatePluginInstance() {
        assertThat(plugin).isNotNull();
        assertThat(plugin.getDescription().getName()).isEqualTo("ChunkSpawnerLimiter");
    }

    @Test
    @DisplayName("Should have plugin components after creation")
    void shouldHavePluginComponentsAfterCreation() {
        assertThat(plugin.getPluginConfig()).isNotNull();
        assertThat(plugin.getCounterDataManager()).isNotNull();
        assertThat(plugin.getRemovalTaskManager()).isNotNull();
        assertThat(plugin.getNotificationService()).isNotNull();
    }
}
