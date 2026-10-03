package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Container;
import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.blocks.Blocks;
import dev.watchwolf.entities.entities.EntityType;
import dev.watchwolf.entities.items.ItemType;
import dev.watchwolf.tester.ExtendedClientPetition;
import dev.watchwolf.tester.TesterConnector;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class WatchWolfTestSupport {
    private static final Duration STATE_TIMEOUT = Duration.ofSeconds(45);

    private WatchWolfTestSupport() {
    }

    static PlayerContext movePlayer(TesterConnector connector, int blockX, int blockZ) throws IOException {
        PlayerContext player = currentPlayer(connector);
        Position current = player.client().getPosition();
        Position destination = new Position(current.getWorld(), blockX + 0.5, current.getBlockY(), blockZ + 0.5);

        connector.server.tp(player.username(), destination);
        return new PlayerContext(player.username(), player.client(), connector.server.getPlayerPosition(player.username()));
    }

    static PlayerContext currentPlayer(TesterConnector connector) throws IOException {
        String username = connector.getClients()[0];
        ExtendedClientPetition client = connector.getClientPetition(username);
        return new PlayerContext(username, client, connector.server.getPlayerPosition(username));
    }

    static void preparePlacement(TesterConnector connector, Position position) throws IOException {
        connector.server.setBlock(position, Blocks.AIR);
        connector.server.setBlock(position.add(0, -1, 0), Blocks.STONE);
    }

    static void awaitEntityCount(TesterConnector connector, Position center, long expected) throws Exception {
        awaitEntityCount(connector, center, EntityType.ARMOR_STAND, expected);
    }

    static void awaitEntityCount(TesterConnector connector, Position center,
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

    static void reloadCsl(TesterConnector connector) throws IOException {
        connector.server.runCommand("csltest reload");
    }

    static void setConfig(TesterConnector connector, String path, String value) throws IOException {
        connector.server.runCommand("csltest set %s %s".formatted(path, value));
    }

    static void awaitInventoryCount(ExtendedClientPetition client, ItemType type, int expected)
            throws Exception {
        await(expected + " " + type.name().toLowerCase(Locale.ROOT) + " in the player's inventory",
                () -> inventoryCount(client.getInventory(), type) == expected);
    }

    static void await(String description, CheckedCondition condition) throws Exception {
        long deadline = System.nanoTime() + STATE_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.evaluate()) {
                return;
            }
            Thread.sleep(100);
        }

        fail("Timed out waiting for " + description);
    }

    private static long countEntities(TesterConnector connector, Position center, EntityType type) throws IOException {
        return Arrays.stream(connector.server.getEntities(center, 8))
                .filter(entity -> entity.getType() == type)
                .count();
    }

    private static int inventoryCount(Container container, ItemType type) {
        return Arrays.stream(container.getItems())
                .filter(item -> item != null && item.getType() == type)
                .mapToInt(item -> Byte.toUnsignedInt(item.getAmount()))
                .sum();
    }

    record PlayerContext(String username, ExtendedClientPetition client, Position position) {
    }

    @FunctionalInterface
    interface CheckedCondition {
        boolean evaluate() throws Exception;
    }
}
