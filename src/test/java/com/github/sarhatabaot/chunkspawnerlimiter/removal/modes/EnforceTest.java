package com.github.sarhatabaot.chunkspawnerlimiter.removal.modes;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Enforce Tests")
class EnforceTest {

    @Test
    @DisplayName("Should cancel blocked placements and resync inventory without adding items")
    void shouldCancelBlockedPlacementsAndResyncInventoryWithoutAddingItems() {
        Enforce enforce = new Enforce(null);
        Block block = mock(Block.class);
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        Player player = mock(Player.class);

        when(event.getPlayer()).thenReturn(player);

        enforce.handleBlock(block, event);

        verify(event).setCancelled(true);
        verify(player).updateInventory();
        verifyNoMoreInteractions(player);
    }
}
