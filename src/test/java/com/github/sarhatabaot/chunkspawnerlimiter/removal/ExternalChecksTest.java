package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("External checks")
class ExternalChecksTest {

    @Test
    @DisplayName("Should cache ignored NBT keys during setup")
    void shouldCacheIgnoredNbtKeysDuringSetup() {
        PluginConfig config = mock(PluginConfig.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Entity entity = mock(Entity.class);
        when(config.getIgnoreNbt()).thenReturn(List.of("protected", "owned"));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            ExternalChecks.setup(config);

            assertThat(ExternalChecks.hasNbtData(entity)).isFalse();
            assertThat(ExternalChecks.hasNbtData(entity)).isFalse();
            verify(config, times(1)).getIgnoreNbt();
        }
    }
}
