package com.github.sarhatabaot.chunkspawnerlimiter;

import dev.watchwolf.tester.TesterConnector;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WatchWolfPerformanceScenarios {
    private static final int CUSTOM_GROUPS = 300;
    private static final int LOADED_CHUNKS = 64;
    private static final int ENTITIES_PER_CHUNK = 16;
    private static final Duration CONFIG_RELOAD_BUDGET = Duration.ofSeconds(5);
    private static final Duration POPULATED_RELOAD_BUDGET = Duration.ofSeconds(10);
    private static final Pattern RESULT = Pattern.compile(
            "CSL stress complete groups=(\\d+) chunks=(\\d+) entities=(\\d+) config-ms=(\\d+) populated-ms=(\\d+)");
    private static final Pattern REPORT_FIELD = Pattern.compile("([a-z0-9-]+)=([^\\s]+)");
    private static final Duration EXTREME_TIMEOUT = Duration.ofMinutes(12);

    private WatchWolfPerformanceScenarios() {
    }

    static void reloadsHighCardinalityConfigurationAndPopulatedChunksWithinBudget(TesterConnector connector)
            throws Exception {
        String output = connector.server.runCommand("csltest stress %d %d %d"
                .formatted(CUSTOM_GROUPS, LOADED_CHUNKS, ENTITIES_PER_CHUNK));
        Matcher result = RESULT.matcher(output);
        assertTrue(result.find(), () -> "Expected CSL stress metrics, but got: " + output);

        assertEquals(CUSTOM_GROUPS, Integer.parseInt(result.group(1)));
        assertEquals(LOADED_CHUNKS, Integer.parseInt(result.group(2)));
        assertEquals(LOADED_CHUNKS * ENTITIES_PER_CHUNK, Integer.parseInt(result.group(3)));

        long configReloadMillis = Long.parseLong(result.group(4));
        long populatedReloadMillis = Long.parseLong(result.group(5));
        assertTrue(configReloadMillis <= CONFIG_RELOAD_BUDGET.toMillis(),
                () -> "High-cardinality config reload took " + configReloadMillis + " ms");
        assertTrue(populatedReloadMillis <= POPULATED_RELOAD_BUDGET.toMillis(),
                () -> "Populated-chunk reload took " + populatedReloadMillis + " ms");
    }

    static void keepsOneThousandChunksLoadedWithoutCrashing(TesterConnector connector) throws Exception {
        Map<String, String> report = runExtremeWorkload(connector, "chunks", 1_000, 0, 10);
        assertEquals("chunks", report.get("scenario"));
        assertEquals(1_000, integer(report, "workload-chunks"));
        assertTrue(integer(report, "loaded-chunks") >= 1_000);
        assertEquals(0, integer(report, "created-entities"));
        assertHealthyServer(report);
    }

    static void handlesTenThousandEntitiesAcrossLoadedChunks(TesterConnector connector) throws Exception {
        Map<String, String> report = runExtremeWorkload(connector, "entities", 1_000, 10, 10);
        assertEquals("entities", report.get("scenario"));
        assertEquals(1_000, integer(report, "workload-chunks"));
        assertEquals(10_000, integer(report, "created-entities"));
        assertTrue(integer(report, "retained-entities") >= 10_000);
        assertHealthyServer(report);
    }

    private static Map<String, String> runExtremeWorkload(TesterConnector connector, String scenario,
                                                           int chunks, int entitiesPerChunk,
                                                           int chunksPerTick) throws Exception {
        String started = connector.server.runCommand("csltest extreme %s %d %d %d"
                .formatted(scenario, chunks, entitiesPerChunk, chunksPerTick));
        assertTrue(started.contains("CSL extreme running"), () -> "Workload did not start: " + started);

        long deadline = System.nanoTime() + EXTREME_TIMEOUT.toNanos();
        String status;
        do {
            Thread.sleep(2_000);
            status = connector.server.runCommand("csltest extreme-status");
            if (status.contains("CSL EXTREME REPORT")) {
                System.out.println(status);
                assertTrue(connector.server.runCommand("version ChunkSpawnerLimiter")
                                .toLowerCase(Locale.ROOT).contains("chunkspawnerlimiter"),
                        "Paper stopped responding after the extreme workload");
                return parseReport(status);
            }
            assertTrue(!status.contains("CSL extreme failed"), "Extreme workload failed: " + status);
        } while (System.nanoTime() < deadline);

        throw new AssertionError("Timed out waiting for extreme workload: " + status);
    }

    private static Map<String, String> parseReport(String output) {
        Map<String, String> fields = new HashMap<>();
        Matcher matcher = REPORT_FIELD.matcher(output);
        while (matcher.find()) {
            fields.put(matcher.group(1), matcher.group(2));
        }
        assertTrue(fields.containsKey("scenario"), () -> "Malformed extreme report: " + output);
        return fields;
    }

    private static void assertHealthyServer(Map<String, String> report) {
        assertTrue(decimal(report, "tps-1m") > 0.0, "Paper did not report a valid TPS value");
        assertTrue(decimal(report, "mspt") >= 0.0, "Paper did not report a valid MSPT value");
        assertTrue(integer(report, "heap-peak-mb") < integer(report, "heap-max-mb"),
                "The server exhausted its configured heap");
    }

    private static int integer(Map<String, String> report, String field) {
        return Integer.parseInt(report.get(field));
    }

    private static double decimal(Map<String, String> report, String field) {
        return Double.parseDouble(report.get(field));
    }
}
