package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.entities.ArmorStand;
import dev.watchwolf.tester.TesterConnector;

import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.awaitEntityCount;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.setConfig;

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
}
