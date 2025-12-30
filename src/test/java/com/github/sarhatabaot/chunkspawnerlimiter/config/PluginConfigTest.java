package com.github.sarhatabaot.chunkspawnerlimiter.config;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for PluginConfig.
 * Tests configuration loading, parsing, and validation functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginConfig Tests")
class PluginConfigTest {

    @Mock
    private org.bukkit.plugin.java.JavaPlugin mockPlugin;

    @Mock
    private org.bukkit.configuration.file.FileConfiguration mockConfig;

    private PluginConfig pluginConfig;

    @BeforeEach
    void setUp() {
        // Mock the plugin to return our mock config
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        doNothing().when(mockPlugin).saveDefaultConfig();

        // Create PluginConfig instance
        pluginConfig = new PluginConfig(mockPlugin);
    }

    @Test
    @DisplayName("Should initialize with default values when config is empty")
    void shouldInitializeWithDefaultsWhenConfigEmpty() {
        // Given
        when(mockConfig.getBoolean("enabled", false)).thenReturn(false);
        when(mockConfig.getBoolean("debug-messages", false)).thenReturn(false);
        when(mockConfig.getBoolean("metrics", true)).thenReturn(true);

        // When - reload is called in constructor

        // Then
        assertThat(pluginConfig.isEnabled()).isFalse();
        assertThat(pluginConfig.isDebugMessages()).isFalse();
        assertThat(pluginConfig.isMetrics()).isTrue();
    }

    @Test
    @DisplayName("Should load entity limits correctly")
    void shouldLoadEntityLimitsCorrectly() {
        // Given
        when(mockConfig.getConfigurationSection("entities.limits"))
            .thenReturn(new MemoryConfiguration().createSection("entities.limits", (Map<String, Object>) (Map) Map.of(
                "ZOMBIE", 10,
                "SKELETON", 5
            )));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).isEqualTo(10);
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.SKELETON)).isEqualTo(5);
        assertThat(pluginConfig.hasResolvedEntityLimit(EntityType.ZOMBIE)).isTrue();
        assertThat(pluginConfig.hasResolvedEntityLimit(EntityType.CREEPER)).isFalse();
    }

    @Test
    @DisplayName("Should load block limits correctly")
    void shouldLoadBlockLimitsCorrectly() {
        // Given
        when(mockConfig.getConfigurationSection("blocks.limits"))
            .thenReturn(new MemoryConfiguration().createSection("blocks.limits", (Map<String, Object>) (Map) Map.of(
                "MOB_SPAWNER", 5,
                "DIAMOND_BLOCK", 10
            )));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getResolvedBlockLimit(Material.MOB_SPAWNER)).isEqualTo(5);
        assertThat(pluginConfig.getResolvedBlockLimit(Material.DIAMOND_BLOCK)).isEqualTo(10);
        assertThat(pluginConfig.hasResolvedBlockLimit(Material.MOB_SPAWNER)).isTrue();
        assertThat(pluginConfig.hasResolvedBlockLimit(Material.STONE)).isFalse();
    }

    @Test
    @DisplayName("Should handle entity groups correctly")
    void shouldHandleEntityGroupsCorrectly() {
        // Given
        when(mockConfig.getConfigurationSection("entities.entity-groups"))
            .thenReturn(new MemoryConfiguration().createSection("entities.entity-groups", (Map<String, Object>) (Map) Map.of(
                "MONSTERS", List.of("ZOMBIE", "SKELETON", "CREEPER")
            )));

        when(mockConfig.getConfigurationSection("entities.limits"))
            .thenReturn(new MemoryConfiguration().createSection("entities.limits", (Map<String, Object>) (Map) Map.of(
                "MONSTERS", 20
            )));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).isEqualTo(20);
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.SKELETON)).isEqualTo(20);
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.CREEPER)).isEqualTo(20);
    }

    @Test
    @DisplayName("Should prioritize direct entity limits over group limits")
    void shouldPrioritizeDirectLimitsOverGroupLimits() {
        // Given
        when(mockConfig.getConfigurationSection("entities.entity-groups"))
            .thenReturn(new MemoryConfiguration().createSection("entities.entity-groups", (Map<String, Object>) (Map) Map.of(
                "MONSTERS", List.of("ZOMBIE", "SKELETON")
            )));

        Map<String, Object> limitsMap = new HashMap<>();
        limitsMap.put("MONSTERS", 20);
        limitsMap.put("ZOMBIE", 5);  // Direct limit should take precedence
        when(mockConfig.getConfigurationSection("entities.limits"))
            .thenReturn(new MemoryConfiguration().createSection("entities.limits", limitsMap));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).isEqualTo(5); // Direct limit wins
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.SKELETON)).isEqualTo(20); // Group limit
    }

    @Test
    @DisplayName("Should load spawn reasons correctly")
    void shouldLoadSpawnReasonsCorrectly() {
        // Given
        when(mockConfig.getStringList("spawn-reasons"))
            .thenReturn(List.of("SPAWNER", "NATURAL", "BREEDING"));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getSpawnReasons())
            .containsExactlyInAnyOrder("SPAWNER", "NATURAL", "BREEDING");
    }

    @Test
    @DisplayName("Should use default spawn reasons when none configured")
    void shouldUseDefaultSpawnReasonsWhenNoneConfigured() {
        // Given
        when(mockConfig.getStringList("spawn-reasons")).thenReturn(null);

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getSpawnReasons()).isNotEmpty();
        assertThat(pluginConfig.getSpawnReasons()).contains("SPAWNER");
    }

    @Test
    @DisplayName("Should parse removal mode correctly")
    void shouldParseRemovalModeCorrectly() {
        // Given
        when(mockConfig.getString("entities.removal.mode", "enforce")).thenReturn("prevent");

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getRemovalMode().getKey()).isEqualTo("prevent");
    }

    @Test
    @DisplayName("Should default to enforce mode for unknown removal modes")
    void shouldDefaultToEnforceModeForUnknownRemovalModes() {
        // Given
        when(mockConfig.getString("entities.removal.mode", "enforce")).thenReturn("unknown");

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getRemovalMode().getKey()).isEqualTo("enforce");
    }

    @Test
    @DisplayName("Should handle world filtering correctly")
    void shouldHandleWorldFilteringCorrectly() {
        // Given
        when(mockConfig.getString("worlds.mode", "excluded")).thenReturn("excluded");
        when(mockConfig.getStringList("worlds.list")).thenReturn(List.of("world_nether", "world_the_end"));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getWorldsMode()).isEqualTo("excluded");
        assertThat(pluginConfig.getWorldsList()).containsExactly("world_nether", "world_the_end");
        assertThat(pluginConfig.isWorldDisabled("world_nether")).isTrue();
        assertThat(pluginConfig.isWorldDisabled("world")).isFalse();
    }

    @Test
    @DisplayName("Should handle notification settings correctly")
    void shouldHandleNotificationSettingsCorrectly() {
        // Given
        when(mockConfig.getBoolean("notifications.enabled", false)).thenReturn(true);
        when(mockConfig.getInt("notifications.cooldown-seconds", 3)).thenReturn(5);
        when(mockConfig.getBoolean("notifications.method.title", true)).thenReturn(true);
        when(mockConfig.getBoolean("notifications.method.message", false)).thenReturn(false);

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.shouldNotifyPlayersInChunk()).isTrue();
        assertThat(pluginConfig.getNotificationCooldownSeconds()).isEqualTo(5);
        assertThat(pluginConfig.shouldUseTitleNotifications()).isTrue();
        assertThat(pluginConfig.shouldUseMessageNotifications()).isFalse();
    }

    @Test
    @DisplayName("Should handle preservation settings correctly")
    void shouldHandlePreservationSettingsCorrectly() {
        // Given
        when(mockConfig.getBoolean("entities.preservation.named-entities", true)).thenReturn(true);
        when(mockConfig.getBoolean("entities.preservation.raid-entities", true)).thenReturn(false);

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.shouldPreserveNamedEntities()).isTrue();
        assertThat(pluginConfig.shouldPreserveRaidEntities()).isFalse();
    }

    @Test
    @DisplayName("Should handle ignore metadata correctly")
    void shouldHandleIgnoreMetadataCorrectly() {
        // Given
        when(mockConfig.getStringList("entities.ignore.metadata"))
            .thenReturn(List.of("shopkeeper", "npc"));

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getIgnoreMetadata())
            .containsExactlyInAnyOrder("shopkeeper", "npc");
    }

    @Test
    @DisplayName("Should use default ignore metadata when none configured")
    void shouldUseDefaultIgnoreMetadataWhenNoneConfigured() {
        // Given
        when(mockConfig.getStringList("entities.ignore.metadata")).thenReturn(null);

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.getIgnoreMetadata()).contains("shopkeeper");
    }

    @Test
    @DisplayName("Should handle inspection settings correctly")
    void shouldHandleInspectionSettingsCorrectly() {
        // Given
        when(mockConfig.getBoolean("events.inspections.enabled", true)).thenReturn(true);
        when(mockConfig.getInt("events.inspections.frequency", 300)).thenReturn(600);
        when(mockConfig.getInt("events.chunk.surrounding-chunks-radius", 1)).thenReturn(2);

        // When
        pluginConfig.reload();

        // Then
        assertThat(pluginConfig.isActiveInspections()).isTrue();
        assertThat(pluginConfig.getInspectionFrequency()).isEqualTo(600);
        assertThat(pluginConfig.getSurroundingChunksRadius()).isEqualTo(2);
    }
}
