# WatchWolf real-server tests

This project runs its integration tests against published WatchWolf `v0.4.1` manager images.
ServersManager starts only the Paper server requested by `src/testWatchWolf/resources/watchwolf.yaml`
through `itzg/minecraft-server`; no WatchWolf source checkout or resource-catalog bootstrap is needed.

## Start

Docker Engine must be running with Linux containers. From the repository root:

```bash
docker compose -f watchwolf/compose.yaml up --wait -d
```

Compose waits until both manager processes are listening without consuming their single-client
sockets. The first test run downloads the requested
Paper version and may take several minutes; later runs reuse Docker's image cache. Manager logs are
stored in the Compose-managed `chunk-spawner-limiter-watchwolf_watchwolf_logs` volume.

## Test

From the repository root:

```bash
./gradlew testWatchWolf
```

On Windows:

```powershell
.\gradlew.bat testWatchWolf
```

Gradle builds the shaded plugin at `build/watchwolf/chunkspawnerlimiter.jar`. WatchWolf requests
Paper 1.20.6, uploads the plugin and a test-only configuration control plugin, runs the real-server
behavior tests, and stops the temporary Minecraft instance afterward. The suite covers plugin
loading, snapshot recounts, block and inventory enforcement, grouped entity and vehicle limits,
cross-chunk movement, spawn-reason filtering, named-entity preservation, runtime reloads, player
preservation, and every removal mode's new-spawn behavior.

The final scenario stress-tests a highly customized installation on the same Paper instance. It
creates 300 entity groups and 300 block groups, loads 64 chunks, spawns 1,024 tracked entities, and
then verifies that both configuration-only and populated-world reloads complete within conservative
regression budgets. The test-only helper measures reload work on Paper's server thread so manager
socket latency is not included in the reported durations.

Two extreme scenarios then keep 1,000 chunks loaded and populate them with 10,000 armor stands in
small per-tick batches. Each prints a `CSL EXTREME REPORT` containing workload duration, CSL reload
duration, loaded chunks, created and retained entities, JVM heap usage, one-minute TPS, and average
MSPT. WatchWolf also writes Paper's complete timings export to
`build/reports/watchwolf-timings/` after the suite finishes.

## Stop

```bash
docker compose -f watchwolf/compose.yaml down
```

To remove the persisted manager logs as well:

```bash
docker compose -f watchwolf/compose.yaml down -v
```
