package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.entities.Position;
import dev.watchwolf.entities.entities.ArmorStand;
import dev.watchwolf.entities.entities.Entity;
import dev.watchwolf.entities.entities.EntityType;
import dev.watchwolf.entities.entities.Minecart;
import dev.watchwolf.entities.entities.Zombie;
import dev.watchwolf.tester.TesterConnector;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.awaitEntityCount;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.movePlayer;
import static com.github.sarhatabaot.chunkspawnerlimiter.WatchWolfTestSupport.setConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WatchWolfEntityScenarios {
    private WatchWolfEntityScenarios() {
    }

    static void enforcesGroupedLimitsPerChunkAndReleasesCapacity(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 256, 0);
        Position center = player.position().getBlockPosition();

        connector.server.spawnEntity(new ArmorStand(center.add(1, 0, 0)));
        connector.server.spawnEntity(new ArmorStand(center.add(2, 0, 0)));
        awaitEntityCount(connector, center, 2);
        connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, 2);

        connector.server.runCommand("kill @e[type=minecraft:armor_stand,x=%d,y=%d,z=%d,distance=..8,limit=1]"
                .formatted(center.getBlockX(), center.getBlockY(), center.getBlockZ()));
        awaitEntityCount(connector, center, 1);
        connector.server.spawnEntity(new ArmorStand(center.add(3, 0, 0)));
        awaitEntityCount(connector, center, 2);

        WatchWolfTestSupport.PlayerContext adjacent = movePlayer(connector, 288, 0);
        Position adjacentCenter = adjacent.position().getBlockPosition();
        connector.server.spawnEntity(new ArmorStand(adjacentCenter.add(1, 0, 0)));
        awaitEntityCount(connector, adjacentCenter, 1);
    }

    static void preservesPlayersWhenPlayerKillingIsDisabled(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 384, 0);
        connector.server.runCommand("csl resync");
        Thread.sleep(1_500);
        assertTrue(Arrays.asList(connector.server.getPlayers()).contains(player.username()));
    }

    static void transfersCapacityAcrossChunkBoundaries(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 320, 0);
        Position source = player.position().getBlockPosition();
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

    static void leavesExcludedSpawnReasonsUntouched(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 512, 0);
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

    static void enforcesVehicleLimitsAndReleasesCapacity(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 448, 0);
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

    static void preservesNamedEntitiesDuringReloadCleanup(TesterConnector connector) throws Exception {
        WatchWolfTestSupport.PlayerContext player = movePlayer(connector, 640, 0);
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
}
