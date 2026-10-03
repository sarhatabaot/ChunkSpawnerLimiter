package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Container;
import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.blocks.Blocks;
import dev.watchwolf.entities.entities.ArmorStand;
import dev.watchwolf.entities.entities.Entity;
import dev.watchwolf.entities.entities.EntityType;
import dev.watchwolf.entities.entities.Minecart;
import dev.watchwolf.entities.entities.Zombie;
import dev.watchwolf.entities.items.Item;
import dev.watchwolf.entities.items.ItemType;
import dev.watchwolf.tester.AbstractTest;
import dev.watchwolf.tester.ExtendedClientPetition;
import dev.watchwolf.tester.TesterConnector;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(WatchWolfSmokeTest.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WatchWolfSmokeTest extends AbstractTest {

    private static final Duration STATE_TIMEOUT = Duration.ofSeconds(45);

    @Override
    public String getConfigFile() {
        return "src/testWatchWolf/resources/watchwolf.yaml";
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(1)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void loadsChunkSpawnerLimiter(TesterConnector connector) throws IOException {
        String response = connector.server.runCommand("version ChunkSpawnerLimiter");

        assertTrue(response.toLowerCase(Locale.ROOT).contains("chunkspawnerlimiter"),
                () -> "Expected Paper to report ChunkSpawnerLimiter as loaded, but got: " + response);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(2)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void recountsCommandPlacedBlocksAfterReload(TesterConnector connector) throws Exception {
        PlayerContext player = currentPlayer(connector);
        Position existing = player.position().add(1, 0, 0).getBlockPosition();
        Position attempted = player.position().add(0, 0, 1).getBlockPosition();
        preparePlacement(connector, existing);
        preparePlacement(connector, attempted);
        connector.server.setBlock(existing, Blocks.WHITE_WOOL);

        reloadCsl(connector);
        Thread.sleep(5_000);

        connector.server.runCommand("clear " + player.username());
        connector.server.giveItem(player.username(), new Item(ItemType.WHITE_WOOL, (byte) 2));
        player.client().setBlock(Blocks.WHITE_WOOL, attempted);
        await("the reloaded snapshot count to reject another wool block",
                () -> Blocks.AIR.equals(connector.server.getBlock(attempted)));
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(3)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesBlockLimitsPerChunkAndReleasesCapacityAfterBreak(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 128, 0);
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

        PlayerContext adjacentChunkPlayer = movePlayer(connector, 160, 0);
        Position adjacentChunkBlock = adjacentChunkPlayer.position().add(1, 0, 0).getBlockPosition();
        preparePlacement(connector, adjacentChunkBlock);

        adjacentChunkPlayer.client().setBlock(Blocks.WHITE_WOOL, adjacentChunkBlock);
        await("the same block type to be allowed in another chunk",
                () -> Blocks.WHITE_WOOL.equals(connector.server.getBlock(adjacentChunkBlock)));
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(4)
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

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(5)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void preservesPlayersWhenPlayerKillingIsDisabled(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 384, 0);

        connector.server.runCommand("csl resync");
        Thread.sleep(1_500);

        assertTrue(Arrays.asList(connector.server.getPlayers()).contains(player.username()));
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(6)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void transfersEntityCapacityWhenEntitiesCrossChunkBoundaries(TesterConnector connector) throws Exception {
        PlayerContext sourcePlayer = movePlayer(connector, 320, 0);
        Position source = sourcePlayer.position().getBlockPosition();
        Position destination = source.add(32, 0, 0);

        Entity moved = connector.server.spawnEntity(new ArmorStand(source.add(1, 0, 0)));
        connector.server.spawnEntity(new ArmorStand(source.add(2, 0, 0)));
        awaitEntityCount(connector, source, 2);

        connector.server.runCommand("tp %s %s %s %s".formatted(
                moved.getUUID(), destination.getX() + 1, destination.getY(), destination.getZ()));
        awaitEntityCount(connector, source, 1);
        awaitEntityCount(connector, destination, 1);

        connector.server.spawnEntity(new ArmorStand(source.add(3, 0, 0)));
        awaitEntityCount(connector, source, 2);
        connector.server.spawnEntity(new ArmorStand(destination.add(2, 0, 0)));
        awaitEntityCount(connector, destination, 2);
        connector.server.spawnEntity(new ArmorStand(destination.add(3, 0, 0)));
        awaitEntityCount(connector, destination, 2);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(7)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void leavesExcludedSpawnReasonsUntouched(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 512, 0);
        Position center = player.position().getBlockPosition();
        connector.server.runCommand("difficulty hard");
        connector.server.runCommand("time set night");

        Entity customSpawn = connector.server.spawnEntity(new Zombie(center.add(1, 0, 0)));
        awaitEntityCount(connector, center, EntityType.ZOMBIE, 1);
        connector.server.runCommand("data merge entity %s {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}"
                .formatted(customSpawn.getUUID()));

        connector.server.runCommand("summon minecraft:zombie %s %s %s {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b}"
                .formatted(center.getX() + 2, center.getY(), center.getZ()));
        awaitEntityCount(connector, center, EntityType.ZOMBIE, 2);

        connector.server.spawnEntity(new Zombie(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, EntityType.ZOMBIE, 2);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(10)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void preservesNamedEntitiesDuringReloadCleanup(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 640, 0);
        Position center = player.position().getBlockPosition();
        try {
            setConfig(connector, "entities.limits.TEST_DECORATIONS", "10");
            Entity named = connector.server.spawnEntity(new ArmorStand(center.add(1, 0, 0)));
            connector.server.runCommand("data merge entity %s {CustomName:'{\"text\":\"Keeper\"}'}"
                    .formatted(named.getUUID()));
            connector.server.spawnEntity(new ArmorStand(center.add(2, 0, 0)));
            connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
            awaitEntityCount(connector, center, 3);

            setConfig(connector, "entities.limits.TEST_DECORATIONS", "1");
            awaitEntityCount(connector, center, 1);
            Set<String> remaining = Arrays.stream(connector.server.getEntities(center, 8))
                    .filter(entity -> entity.getType() == EntityType.ARMOR_STAND)
                    .map(Entity::getUUID)
                    .collect(Collectors.toSet());
            assertTrue(remaining.contains(named.getUUID()), "The named armor stand should be preserved");
        } finally {
            setConfig(connector, "entities.limits.TEST_DECORATIONS", "2");
        }
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(8)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesVehicleLimitsAndReleasesCapacityAfterDestruction(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 448, 0);
        Position center = player.position().getBlockPosition();

        connector.server.spawnEntity(new Minecart(center.add(1, 0, 0)));
        awaitEntityCount(connector, center, EntityType.MINECART, 1);
        connector.server.spawnEntity(new Minecart(center.add(2, 0, 0)));
        awaitEntityCount(connector, center, EntityType.MINECART, 1);

        connector.server.runCommand("kill @e[type=minecraft:minecart,x=%s,y=%s,z=%s,distance=..8,limit=1]"
                .formatted(center.getBlockX(), center.getBlockY(), center.getBlockZ()));
        awaitEntityCount(connector, center, EntityType.MINECART, 0);
        connector.server.spawnEntity(new Minecart(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, EntityType.MINECART, 1);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(9)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void appliesChangedLimitsThroughTheNormalReloadCommand(TesterConnector connector) throws Exception {
        PlayerContext player = movePlayer(connector, 704, 0);
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

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(11)
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void appliesEveryRemovalModeToNewViolations(TesterConnector connector) throws Exception {
        String[] modes = {"prevent", "remove", "kill", "enforce", "enforce-kill"};
        try {
            for (int index = 0; index < modes.length; index++) {
                String mode = modes[index];
                Position spawnCenter = new Position("world", 768 + index * 32 + 0.5, -60, 0.5);

                setConfig(connector, "entities.removal.mode", mode);
                connector.server.spawnEntity(new ArmorStand(spawnCenter.add(1, 0, 0)));
                awaitEntityCount(connector, spawnCenter, 1);
                connector.server.spawnEntity(new ArmorStand(spawnCenter.add(2, 0, 0)));
                awaitEntityCount(connector, spawnCenter, 2);
                connector.server.spawnEntity(new ArmorStand(spawnCenter.add(3, 0, 0)));
                awaitEntityCount(connector, spawnCenter, 2);
            }
        } finally {
            setConfig(connector, "entities.removal.mode", "enforce");
        }
    }

    private static PlayerContext movePlayer(TesterConnector connector, int blockX, int blockZ) throws IOException {
        PlayerContext player = currentPlayer(connector);
        Position current = player.client().getPosition();
        Position destination = new Position(current.getWorld(), blockX + 0.5, current.getBlockY(), blockZ + 0.5);

        connector.server.tp(player.username(), destination);
        return new PlayerContext(player.username(), player.client(), connector.server.getPlayerPosition(player.username()));
    }

    private static PlayerContext currentPlayer(TesterConnector connector) throws IOException {
        String username = connector.getClients()[0];
        ExtendedClientPetition client = connector.getClientPetition(username);
        return new PlayerContext(username, client, connector.server.getPlayerPosition(username));
    }

    private static void preparePlacement(TesterConnector connector, Position position) throws IOException {
        connector.server.setBlock(position, Blocks.AIR);
        connector.server.setBlock(position.add(0, -1, 0), Blocks.STONE);
    }

    private static void awaitEntityCount(TesterConnector connector, Position center, long expected) throws Exception {
        awaitEntityCount(connector, center, EntityType.ARMOR_STAND, expected);
    }

    private static void awaitEntityCount(TesterConnector connector, Position center,
                                         EntityType type, long expected) throws Exception {
        long deadline = System.nanoTime() + STATE_TIMEOUT.toNanos();
        long actual;
        do {
            actual = countEntities(connector, center, type);
            if (actual == expected) {
                assertEquals(expected, actual);
                return;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);

        fail("Timed out waiting for %d %s entities near %s; observed %d"
                .formatted(expected, type.name().toLowerCase(Locale.ROOT), center, actual));
    }

    private static long countEntities(TesterConnector connector, Position center, EntityType type) throws IOException {
        return Arrays.stream(connector.server.getEntities(center, 8))
                .filter(entity -> entity.getType() == type)
                .count();
    }

    private static void reloadCsl(TesterConnector connector) throws IOException {
        connector.server.runCommand("csltest reload");
    }

    private static void setConfig(TesterConnector connector, String path, String value) throws IOException {
        connector.server.runCommand("csltest set %s %s".formatted(path, value));
    }

    private static void awaitInventoryCount(ExtendedClientPetition client, ItemType type, int expected)
            throws Exception {
        await(expected + " " + type.name().toLowerCase(Locale.ROOT) + " in the player's inventory",
                () -> inventoryCount(client.getInventory(), type) == expected);
    }

    private static int inventoryCount(Container container, ItemType type) {
        return Arrays.stream(container.getItems())
                .filter(item -> item != null && item.getType() == type)
                .mapToInt(item -> Byte.toUnsignedInt(item.getAmount()))
                .sum();
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
