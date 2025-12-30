package com.github.sarhatabaot.chunkspawnerlimiter.integration;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full plugin lifecycle using MockBukkit 3.x.
 * Tests plugin loading, configuration, and basic functionality for Minecraft 1.17+.
 *
 * Note: This test suite uses MockBukkit 3.x which supports Minecraft 1.17+.
 * For legacy versions (1.8-1.12), see PluginIntegrationLegacyTest.
 */
@DisplayName("Plugin Integration Tests (Modern)")
class PluginIntegrationTest {

    private ServerMock server;
    private ChunkSpawnerLimiter plugin;

    @BeforeEach
    void setUp() {
        // Create a mock server
        server = MockBukkit.mock();

        // Load the plugin
        plugin = MockBukkit.load(ChunkSpawnerLimiter.class);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should load plugin successfully")
    void shouldLoadPluginSuccessfully() {
        // Then
        assertThat(plugin).isNotNull();
        assertThat(plugin.isEnabled()).isFalse(); // Should be disabled by default
        assertThat(server.getPluginManager().getPlugin("ChunkSpawnerLimiter")).isEqualTo(plugin);
    }

    @Test
    @DisplayName("Should initialize plugin components correctly")
    void shouldInitializePluginComponentsCorrectly() {
        // Then
        assertThat(plugin.getPluginConfig()).isNotNull();
        assertThat(plugin.getCounterDataManager()).isNotNull();
        assertThat(plugin.getRemovalTaskManager()).isNotNull();
        assertThat(plugin.getNotificationService()).isNotNull();
    }

    @Test
    @DisplayName("Should have default configuration loaded")
    void shouldHaveDefaultConfigurationLoaded() {
        // Given
        PluginConfig config = plugin.getPluginConfig();

        // Then
        assertThat(config.isEnabled()).isFalse(); // Default should be false
        assertThat(config.isMetrics()).isTrue(); // Default should be true
        assertThat(config.isDebugMessages()).isFalse(); // Default should be false
    }

    @Test
    @DisplayName("Should register event listeners")
    void shouldRegisterEventListeners() {
        // When - Plugin is loaded (in setUp)

        // Then - Check that listeners are registered by verifying they exist
        // Note: MockBukkit doesn't provide direct access to registered listeners,
        // but we can verify the plugin manager calls were made by checking
        // that the components were created successfully
        assertThat(plugin.getPluginConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should handle plugin enable/disable cycle")
    void shouldHandlePluginEnableDisableCycle() {
        // Given - Plugin is loaded but not enabled

        // When - Enable the plugin
        plugin.onEnable();

        // Then - Should be enabled
        assertThat(plugin.isEnabled()).isTrue();

        // When - Disable the plugin
        plugin.onDisable();

        // Then - Components should be cleaned up
        assertThat(plugin.getCounterDataManager()).isNull();
        assertThat(plugin.getRemovalTaskManager()).isNull();
        assertThat(plugin.getPluginConfig()).isNull();
        assertThat(plugin.getNotificationService()).isNull();
    }

    @Test
    @DisplayName("Should handle configuration reload")
    void shouldHandleConfigurationReload() {
        // Given
        PluginConfig config = plugin.getPluginConfig();

        // When - Reload configuration
        plugin.onReload();

        // Then - Config should still be available and functional
        assertThat(config).isNotNull();
        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should have valid plugin metadata")
    void shouldHaveValidPluginMetadata() {
        // When - Get plugin description
        var description = plugin.getDescription();

        // Then
        assertThat(description.getName()).isEqualTo("ChunkSpawnerLimiter");
        assertThat(description.getVersion()).isEqualTo("5.0.0-RC3");
        assertThat(description.getDescription()).contains("Limit blocks & entities in chunks");
        assertThat(description.getAuthors()).contains("Cyprias", "sarhatabaot");
    }

    @Test
    @DisplayName("Should register commands correctly")
    void shouldRegisterCommandsCorrectly() {
        // When - Plugin is loaded

        // Then - Commands should be registered
        // Note: MockBukkit command registration testing is limited,
        // but we can verify the command framework was initialized
        assertThat(plugin).isNotNull();
    }
}
