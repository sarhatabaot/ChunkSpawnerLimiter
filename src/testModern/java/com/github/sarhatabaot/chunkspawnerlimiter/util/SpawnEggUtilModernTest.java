package com.github.sarhatabaot.chunkspawnerlimiter.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpawnEggUtil Tests (Modern)")
class SpawnEggUtilModernTest {

    @Test
    @DisplayName("Should return null when entity type is null")
    void shouldReturnNullWhenEntityTypeIsNull() {
        assertThat(SpawnEggUtil.getSpawnEggMaterial(null)).isNull();
    }

    @Test
    @DisplayName("Should return modern spawn egg material for known entity")
    void shouldReturnModernSpawnEggMaterialForKnownEntity() {
        assertThat(SpawnEggUtil.getSpawnEggMaterial(EntityType.ZOMBIE))
                .isEqualTo(Material.ZOMBIE_SPAWN_EGG);
    }
}