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

## Stop

```bash
docker compose -f watchwolf/compose.yaml down
```

To remove the persisted manager logs as well:

```bash
docker compose -f watchwolf/compose.yaml down -v
```
