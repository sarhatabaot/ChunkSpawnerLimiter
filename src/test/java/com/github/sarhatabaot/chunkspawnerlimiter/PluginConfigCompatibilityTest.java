package com.github.sarhatabaot.chunkspawnerlimiter;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
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

    private PluginConfig pluginConfig;

    @BeforeEach
    void setUp() {
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getServer()).thenReturn(server);
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
}
