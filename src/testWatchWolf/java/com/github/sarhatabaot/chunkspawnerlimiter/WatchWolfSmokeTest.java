package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.core.entities.Position;
import dev.watchwolf.core.entities.blocks.Blocks;
import dev.watchwolf.core.entities.entities.ArmorStand;
import dev.watchwolf.core.entities.entities.EntityType;
import dev.watchwolf.core.entities.items.Item;
import dev.watchwolf.core.entities.items.ItemType;
import dev.watchwolf.tester.AbstractTest;
import dev.watchwolf.tester.ExtendedClientPetition;
import dev.watchwolf.tester.TesterConnector;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(WatchWolfSmokeTest.class)
class WatchWolfSmokeTest extends AbstractTest {

    private static final Duration STATE_TIMEOUT = Duration.ofSeconds(15);

    @Override
    public String getConfigFile() {
        return "src/testWatchWolf/resources/watchwolf.yaml";
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void loadsChunkSpawnerLimiter(TesterConnector connector) throws IOException {
        String response = connector.server.runCommand("version ChunkSpawnerLimiter");

        assertTrue(response.toLowerCase(Locale.ROOT).contains("chunkspawnerlimiter"),
                () -> "Expected Paper to report ChunkSpawnerLimiter as loaded, but got: " + response);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesBlockLimitsPerChunkAndReleasesCapacityAfterBreak(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 128, 0);
        Position first = player.position().add(1, 0, 0).getBlockPosition();
        Position second = player.position().add(0, 0, 1).getBlockPosition();

        preparePlacement(connector, first);
        preparePlacement(connector, second);
        connector.server.giveItem(player.username(), new Item(ItemType.WHITE_WOOL, (byte) 4));

        player.client().setBlock(Blocks.WHITE_WOOL, first);
        await("the first white wool block to be placed",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(first)));

        player.client().setBlock(Blocks.WHITE_WOOL, second);
        await("the second white wool block to be rejected",
                () -> Blocks.AIR.equals(connector.server.getBlock(second)));

        player.client().breakBlock(first);
        await("breaking the first block to free capacity",
                () -> Blocks.AIR.equals(connector.server.getBlock(first)));

        player.client().setBlock(Blocks.WHITE_WOOL, second);
        await("placement to succeed after capacity was released",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(second)));

        PlayerContext adjacentChunkPlayer = movePlayer(connector, 160, 0);
        Position adjacentChunkBlock = adjacentChunkPlayer.position().add(1, 0, 0).getBlockPosition();
        preparePlacement(connector, adjacentChunkBlock);

        adjacentChunkPlayer.client().setBlock(Blocks.WHITE_WOOL, adjacentChunkBlock);
        await("the same block type to be allowed in another chunk",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(adjacentChunkBlock)));
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesGroupedEntityLimitsPerChunkAndReleasesCapacityAfterDeath(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 256, 0);
        Position center = player.position().getBlockPosition();

        connector.server.spawnEntity(new ArmorStand(center.add(1, 0, 0)));
        connector.server.spawnEntity(new ArmorStand(center.add(2, 0, 0)));
        awaitEntityCount(connector, center, 2);

        connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, 2);

        String killCommand = "kill @e[type=minecraft:armor_stand,x=%d,y=%d,z=%d,distance=..8,limit=1]"
                .formatted(center.getBlockX(), center.getBlockY(), center.getBlockZ());
        connector.server.runCommand(killCommand);
        awaitEntityCount(connector, center, 1);

        connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, 2);

        PlayerContext adjacentChunkPlayer = movePlayer(connector, 288, 0);
        Position adjacentCenter = adjacentChunkPlayer.position().getBlockPosition();
        connector.server.spawnEntity(new ArmorStand(adjacentCenter.add(1, 0, 0)));
        awaitEntityCount(connector, adjacentCenter, 1);
    }

    private static PlayerContext movePlayer(TesterConnector connector, int blockX, int blockZ) throws IOException {
        String username = connector.getClients()[0];
        ExtendedClientPetition client = connector.getClientPetition(username);
        Position current = client.getPosition();
        Position destination = new Position(current.getWorld(), blockX + 0.5, current.getBlockY(), blockZ + 0.5);

        connector.server.tp(username, destination);
        return new PlayerContext(username, client, connector.server.getPlayerPosition(username));
    }

    private static void preparePlacement(TesterConnector connector, Position position) throws IOException {
        connector.server.setBlock(position, Blocks.AIR);
        connector.server.setBlock(position.add(0, -1, 0), Blocks.STONE);
    }

    private static void awaitEntityCount(TesterConnector connector, Position center, long expected) throws Exception {
        await(expected + " armor stands near " + center,
                () -> countEntities(connector, center, EntityType.ARMOR_STAND) == expected);
        assertEquals(expected, countEntities(connector, center, EntityType.ARMOR_STAND));
    }

    private static long countEntities(TesterConnector connector, Position center, EntityType type) throws IOException {
        return Arrays.stream(connector.server.getEntities(center, 8))
                .filter(entity -> entity.getType() == type)
                .count();
    }

    private static void await(String description, CheckedCondition condition) throws Exception {
        long deadline = System.nanoTime() + STATE_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.evaluate()) {
                return;
            }
            Thread.sleep(100);
        }

        fail("Timed out waiting for " + description);
    }

    private record PlayerContext(String username, ExtendedClientPetition client, Position position) {
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean evaluate() throws Exception;
    }
}
