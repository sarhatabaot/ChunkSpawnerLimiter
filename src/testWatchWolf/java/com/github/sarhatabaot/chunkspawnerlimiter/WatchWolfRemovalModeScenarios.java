package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.entities.ArmorStand;
import dev.watchwolf.entities.entities.EntityType;
import dev.watchwolf.tester.TesterConnector;

import java.util.Arrays;

import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.awaitEntityCount;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.movePlayer;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.setConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WatchWolfRemovalModeScenarios {
    private WatchWolfRemovalModeScenarios() {
    }

    static void appliesEveryModeToNewViolations(TesterConnector connector) throws Exception {
        String[] modes = {"prevent", "remove", "kill", "enforce", "enforce-kill"};
        try {
            for (int index = 0; index < modes.length; index++) {
                Position center = new Position("world", 768 + index * 32 + 0.5, -60, 0.5);
                setConfig(connector, "entities.removal.mode", modes[index]);
                connector.server.spawnEntity(new ArmorStand(center.add(1, 0, 0)));
                awaitEntityCount(connector, center, 1);
                connector.server.spawnEntity(new ArmorStand(center.add(2, 0, 0)));
                awaitEntityCount(connector, center, 2);
                connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
                awaitEntityCount(connector, center, 2);
            }
        } finally {
            setConfig(connector, "entities.removal.mode", "enforce");
        }
    }

    static void appliesEveryModeToExistingEntitiesAfterReload(TesterConnector connector) throws Exception {
        String[] modes = {"remove", "kill", "enforce", "enforce-kill", "prevent"};
        try {
            for (int index = 0; index < modes.length; index++) {
                WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 1024 + index * 32, 0);
                Position center = player.position().getBlockPosition();
                setConfig(connector, "entities.limits.TEST_DECORATIONS", "10");
                connector.server.spawnEntity(new ArmorStand(center.add(1, 0, 0)));
                connector.server.spawnEntity(new ArmorStand(center.add(2, 0, 0)));
                awaitEntityCount(connector, center, 2);

                setConfig(connector, "entities.removal.mode", modes[index]);
                setConfig(connector, "entities.limits.TEST_DECORATIONS", "1");
                long expected = modes[index].equals("prevent") ? 2 : 1;
                awaitEntityCount(connector, center, expected);
                Thread.sleep(2_500L);
                long actual = Arrays.stream(connector.server.getEntities(center, 8))
                        .filter(entity -> entity.getType() == EntityType.ARMOR_STAND)
                        .count();
                assertEquals(expected, actual, "Unexpected armor stand count after reload in " + modes[index] + " mode");
            }
        } finally {
            setConfig(connector, "entities.limits.TEST_DECORATIONS", "2");
            setConfig(connector, "entities.removal.mode", "enforce");
        }
    }
}
