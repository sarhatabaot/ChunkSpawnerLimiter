package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.blocks.Blocks;
import dev.watchwolf.entities.items.Item;
import dev.watchwolf.entities.items.ItemType;
import dev.watchwolf.tester.TesterConnector;

import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.await;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.awaitInventoryCount;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.currentPlayer;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.movePlayer;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.preparePlacement;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.reloadCsl;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.setConfig;

final class WatchWolfBlockScenarios {
    private WatchWolfBlockScenarios() {
    }

    static void recountsCommandPlacedBlocksAfterReload(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = currentPlayer(connector);
        Position existing = player.position().add(1, 0, 0).getBlockPosition();
        Position attempted = player.position().add(0, 0, 1).getBlockPosition();
        preparePlacement(connector, existing);
        preparePlacement(connector, attempted);
        connector.server.setBlock(existing, Blocks.WHITE_WOOL);

        reloadCsl(connector);
        connector.server.runCommand("clear " + player.username());
        await("the reloaded snapshot count to reject another wool block", () -> {
            connector.server.setBlock(attempted, Blocks.AIR);
            connector.server.giveItem(player.username(), new Item(ItemType.WHITE_WOOL, (byte) 1));
            player.client().setBlock(Blocks.WHITE_WOOL, attempted);
            Thread.sleep(250);
            return Blocks.AIR.equals(connector.server.getBlock(attempted));
        });
    }

    static void enforcesLimitsPerChunkAndReleasesCapacity(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 128, 0);
        Position first = player.position().add(1, 0, 0).getBlockPosition();
        Position second = player.position().add(0, 0, 1).getBlockPosition();

        preparePlacement(connector, first);
        preparePlacement(connector, second);
        connector.server.runCommand("clear " + player.username());
        connector.server.giveItem(player.username(), new Item(ItemType.WHITE_WOOL, (byte) 4));
        awaitInventoryCount(player.client(), ItemType.WHITE_WOOL, 4);

        player.client().setBlock(Blocks.WHITE_WOOL, first);
        await("the first white wool block to be placed",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(first)));
        awaitInventoryCount(player.client(), ItemType.WHITE_WOOL, 3);

        player.client().setBlock(Blocks.WHITE_WOOL, second);
        await("the second white wool block to be rejected",
                () -> Blocks.AIR.equals(connector.server.getBlock(second)));
        awaitInventoryCount(player.client(), ItemType.WHITE_WOOL, 3);

        player.client().breakBlock(first);
        await("breaking the first block to free capacity",
                () -> Blocks.AIR.equals(connector.server.getBlock(first)));
        player.client().setBlock(Blocks.WHITE_WOOL, second);
        await("placement to succeed after capacity was released",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(second)));

        WatchWolfTestSupport.PlayerContext adjacent = movePlayer(connector, 160, 0);
        Position adjacentBlock = adjacent.position().add(1, 0, 0).getBlockPosition();
        preparePlacement(connector, adjacentBlock);
        adjacent.client().setBlock(Blocks.WHITE_WOOL, adjacentBlock);
        await("the same block type to be allowed in another chunk",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(adjacentBlock)));
    }

    static void appliesChangedLimitsThroughReload(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 704, 0);
        Position first = player.position().add(1, 0, 0).getBlockPosition();
        Position second = player.position().add(0, 0, 1).getBlockPosition();
        preparePlacement(connector, first);
        preparePlacement(connector, second);
        connector.server.runCommand("clear " + player.username());
        connector.server.giveItem(player.username(), new Item(ItemType.WHITE_WOOL, (byte) 4));

        player.client().setBlock(Blocks.WHITE_WOOL, first);
        await("the first reload-test block to be placed",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(first)));
        player.client().setBlock(Blocks.WHITE_WOOL, second);
        await("the original limit to reject a second reload-test block",
                () -> Blocks.AIR.equals(connector.server.getBlock(second)));

        try {
            setConfig(connector, "blocks.limits.TEST_BLOCKS", "2");
            Thread.sleep(3_000);
            player.client().setBlock(Blocks.WHITE_WOOL, second);
            await("the reloaded block limit to allow a second block",
                    () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(second)));
        } finally {
            setConfig(connector, "blocks.limits.TEST_BLOCKS", "1");
        }
    }
}
