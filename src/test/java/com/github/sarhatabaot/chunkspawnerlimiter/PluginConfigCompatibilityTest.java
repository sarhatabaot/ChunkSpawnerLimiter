package com.github.sarhatabaot.chunkspawnerlimiter;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PluginConfig Compatibility Tests")
class PluginConfigCompatibilityTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private FileConfiguration config;

    @Mock
    private Server server;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private Logger logger;

    private PluginConfig pluginConfig;

    @BeforeEach
    void setUp() {
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(logger);
        when(server.getPluginManager()).thenReturn(pluginManager);
        doNothing().when(plugin).saveDefaultConfig();

        pluginConfig = new PluginConfig(plugin);
    }

    @Test
    @DisplayName("Should auto-enable deferred spawn counting when WildStacker is present")
    void shouldAutoEnableDeferredSpawnCountingWhenWildStackerIsPresent() {
        when(config.getString("entities.compatibility.defer-count-until-next-tick", "auto")).thenReturn("auto");
        when(pluginManager.isPluginEnabled("WildStacker")).thenReturn(true);

        pluginConfig.reload();

        assertThat(pluginConfig.shouldDelayEntityCountForCompatibility()).isTrue();
    }

    @Test
    @DisplayName("Should allow explicitly disabling deferred spawn counting")
    void shouldAllowExplicitlyDisablingDeferredSpawnCounting() {
        when(config.getString("entities.compatibility.defer-count-until-next-tick", "auto")).thenReturn("false");
        when(pluginManager.isPluginEnabled("WildStacker")).thenReturn(true);

        pluginConfig.reload();

        assertThat(pluginConfig.shouldDelayEntityCountForCompatibility()).isFalse();
    }

    @Test
    @DisplayName("Should skip invalid block group members")
    void shouldSkipInvalidBlockGroupMembers() {
        when(config.getConfigurationSection("blocks.block-groups"))
                .thenReturn(new MemoryConfiguration().createSection("blocks.block-groups", Map.of(
                        "STORAGE", List.of("DIAMOND_BLOCK", "NOT_A_BLOCK")
                )));
        when(config.getConfigurationSection("blocks.limits"))
                .thenReturn(new MemoryConfiguration().createSection("blocks.limits", Map.of(
                        "STORAGE", 4
                )));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedBlockLimit(Material.DIAMOND_BLOCK)).isEqualTo(4);
    }

    @Test
    @DisplayName("Should skip invalid entity group members")
    void shouldSkipInvalidEntityGroupMembers() {
        when(config.getConfigurationSection("entities.entity-groups"))
                .thenReturn(new MemoryConfiguration().createSection("entities.entity-groups", Map.of(
                        "HOSTILE", List.of("ZOMBIE", "NOT_AN_ENTITY")
                )));
        when(config.getConfigurationSection("entities.limits"))
                .thenReturn(new MemoryConfiguration().createSection("entities.limits", Map.of(
                        "HOSTILE", 6
                )));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).isEqualTo(6);
    }

    @Test
    @DisplayName("Should normalize configured names regardless of case")
    void shouldNormalizeConfiguredNamesRegardlessOfCase() {
        when(config.getConfigurationSection("blocks.block-groups"))
                .thenReturn(new MemoryConfiguration().createSection("blocks.block-groups", Map.of(
                        "Storage", List.of("diamond_block")
                )));
        when(config.getConfigurationSection("blocks.limits"))
                .thenReturn(new MemoryConfiguration().createSection("blocks.limits", Map.of(
                        "storage", 4
                )));
        when(config.getConfigurationSection("entities.entity-groups"))
                .thenReturn(new MemoryConfiguration().createSection("entities.entity-groups", Map.of(
                        "Hostile", List.of("zombie")
                )));
        when(config.getConfigurationSection("entities.limits"))
                .thenReturn(new MemoryConfiguration().createSection("entities.limits", Map.of(
                        "hostile", 6,
                        "skeleton", 2
                )));
        when(config.getStringList("spawn-reasons"))
                .thenReturn(List.of("natural", "not_a_reason"));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedBlockLimit(Material.DIAMOND_BLOCK)).isEqualTo(4);
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.ZOMBIE)).isEqualTo(6);
        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.SKELETON)).isEqualTo(2);
        assertThat(pluginConfig.getSpawnReasons()).containsExactly("NATURAL");
    }

    @Test
    @DisplayName("Should warn about misspelled entity limit keys")
    void shouldWarnAboutMisspelledEntityLimitKeys() {
        when(config.getConfigurationSection("entities.limits"))
                .thenReturn(new MemoryConfiguration().createSection("entities.limits", Map.of(
                        "CHIKEN", 5
                )));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.CHICKEN)).isNull();
        verify(logger).warning(contains(
                "Unknown entity type or group 'CHIKEN' at entities.limits.CHIKEN; skipping it. Did you mean 'CHICKEN'?"));
    }

    @Test
    @DisplayName("Should warn about misspelled block limit keys")
    void shouldWarnAboutMisspelledBlockLimitKeys() {
        when(config.getConfigurationSection("blocks.limits"))
                .thenReturn(new MemoryConfiguration().createSection("blocks.limits", Map.of(
                        "DIAMON_BLOCK", 3
                )));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedBlockLimit(Material.DIAMOND_BLOCK)).isNull();
        verify(logger).warning(contains(
                "Unknown block material or group 'DIAMON_BLOCK' at blocks.limits.DIAMON_BLOCK; skipping it. Did you mean 'DIAMOND_BLOCK'?"));
    }

    @Test
    @DisplayName("Should accept configured group limit keys")
    void shouldAcceptConfiguredGroupLimitKeys() {
        when(config.getConfigurationSection("entities.entity-groups"))
                .thenReturn(new MemoryConfiguration().createSection("entities.entity-groups", Map.of(
                        "BIRDS", List.of("CHICKEN")
                )));
        when(config.getConfigurationSection("entities.limits"))
                .thenReturn(new MemoryConfiguration().createSection("entities.limits", Map.of(
                        "BIRDS", 5
                )));

        pluginConfig.reload();

        assertThat(pluginConfig.getResolvedEntityLimit(EntityType.CHICKEN)).isEqualTo(5);
        verify(logger, never()).warning(contains("entities.limits.BIRDS"));
    }
}
