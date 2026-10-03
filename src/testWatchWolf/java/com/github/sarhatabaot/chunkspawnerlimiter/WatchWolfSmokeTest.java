package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.tester.AbstractTest;
import dev.watchwolf.tester.TesterConnector;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WatchWolfSmokeTest.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("java:S2699")
class WatchWolfSmokeTest extends AbstractTest {
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
        WatchWolfBlockScenarios.recountsCommandPlacedBlocksAfterReload(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(3)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesBlockLimitsPerChunk(TesterConnector connector) throws Exception {
        WatchWolfBlockScenarios.enforcesLimitsPerChunkAndReleasesCapacity(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(4)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesGroupedEntityLimitsPerChunk(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.enforcesGroupedLimitsPerChunkAndReleasesCapacity(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(5)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void preservesPlayersWhenPlayerKillingIsDisabled(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.preservesPlayersWhenPlayerKillingIsDisabled(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(6)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void transfersEntityCapacityAcrossChunks(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.transfersCapacityAcrossChunkBoundaries(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(7)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void leavesExcludedSpawnReasonsUntouched(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.leavesExcludedSpawnReasonsUntouched(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(8)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void enforcesVehicleLimits(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.enforcesVehicleLimitsAndReleasesCapacity(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(9)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void appliesChangedLimitsThroughReload(TesterConnector connector) throws Exception {
        WatchWolfBlockScenarios.appliesChangedLimitsThroughReload(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(10)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    void preservesNamedEntitiesDuringReloadCleanup(TesterConnector connector) throws Exception {
        WatchWolfEntityScenarios.preservesNamedEntitiesDuringReloadCleanup(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(11)
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void appliesEveryRemovalMode(TesterConnector connector) throws Exception {
        WatchWolfRemovalModeScenarios.appliesEveryModeToNewViolations(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(12)
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void stressTestsCustomConfigurationAndLoadedChunks(TesterConnector connector) throws Exception {
        WatchWolfPerformanceScenarios.reloadsHighCardinalityConfigurationAndPopulatedChunksWithinBudget(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(13)
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void keepsOneThousandChunksLoaded(TesterConnector connector) throws Exception {
        WatchWolfPerformanceScenarios.keepsOneThousandChunksLoadedWithoutCrashing(connector);
    }

    @ParameterizedTest
    @ArgumentsSource(WatchWolfSmokeTest.class)
    @Order(14)
    @Timeout(value = 15, unit = TimeUnit.MINUTES)
    void handlesTenThousandEntities(TesterConnector connector) throws Exception {
        WatchWolfPerformanceScenarios.handlesTenThousandEntitiesAcrossLoadedChunks(connector);
    }
}
