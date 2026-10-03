package com.github.sarhatabaot.chunkspawnerlimiter.removal;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Entity eligibility checks")
class ChecksTest {

    @Test
    @DisplayName("Should exclude players when player killing is disabled")
    void shouldExcludePlayersWhenPlayerKillingIsDisabled() {
        PluginConfig config = mock(PluginConfig.class);
        Player player = mock(Player.class);
        when(player.getType()).thenReturn(EntityType.PLAYER);
        when(config.hasResolvedEntityLimit(EntityType.PLAYER)).thenReturn(true);
        when(config.isKillPlayers()).thenReturn(false);

        assertThat(Checks.shouldTrackEntity(player, config)).isFalse();
    }

    @Test
    @DisplayName("Should include players only when configured and player killing is enabled")
    void shouldIncludePlayersOnlyWhenConfiguredAndPlayerKillingIsEnabled() {
        PluginConfig config = mock(PluginConfig.class);
        Player player = mock(Player.class);
        when(player.getType()).thenReturn(EntityType.PLAYER);
        when(config.hasResolvedEntityLimit(EntityType.PLAYER)).thenReturn(true);
        when(config.isKillPlayers()).thenReturn(true);

        assertThat(Checks.shouldTrackEntity(player, config)).isTrue();
    }

    @Test
    @DisplayName("Should include configured non-player entities")
    void shouldIncludeConfiguredNonPlayerEntities() {
        PluginConfig config = mock(PluginConfig.class);
        Entity zombie = mock(Entity.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(config.hasResolvedEntityLimit(EntityType.ZOMBIE)).thenReturn(true);

        assertThat(Checks.shouldTrackEntity(zombie, config)).isTrue();
    }

    @Test
    @DisplayName("Should exclude entities without a configured limit")
    void shouldExcludeEntitiesWithoutAConfiguredLimit() {
        PluginConfig config = mock(PluginConfig.class);
        Entity zombie = mock(Entity.class);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);

        assertThat(Checks.shouldTrackEntity(zombie, config)).isFalse();
    }
}
