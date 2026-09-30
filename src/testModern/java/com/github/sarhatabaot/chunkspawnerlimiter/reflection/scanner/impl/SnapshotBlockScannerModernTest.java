package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Modern SnapshotBlockScanner Tests")
class SnapshotBlockScannerModernTest {

    @Test
    @DisplayName("Should count tracked materials through the modern snapshot API")
    void shouldCountModernSnapshotMaterials() {
        ChunkSnapshot snapshot = mock(ChunkSnapshot.class);
        when(snapshot.getBlockType(anyInt(), anyInt(), anyInt())).thenReturn(Material.AIR);
        when(snapshot.getBlockType(1, -1, 2)).thenReturn(Material.DIAMOND_BLOCK);
        when(snapshot.getBlockType(1, 0, 2)).thenReturn(Material.DIAMOND_BLOCK);

        Map<Material, Integer> counts = SnapshotBlockScanner.countTrackedBlocks(
                snapshot, -1, 1, Set.of(Material.DIAMOND_BLOCK));

        assertThat(counts).containsEntry(Material.DIAMOND_BLOCK, 2).hasSize(1);
    }
}
