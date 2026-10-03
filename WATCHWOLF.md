# WatchWolf Integration Tests

The `testWatchWolf` Gradle suite runs ChunkSpawnerLimiter against a real Paper server and a real automated client. It is intentionally separate from `check` because it requires a running WatchWolf environment.

## Environment setup

WatchWolf requires Ubuntu and Docker. On Windows, run it inside WSL2 with Docker Desktop integration enabled.

```bash
wget https://raw.githubusercontent.com/watch-wolf/WatchWolf/dev/WatchWolfSetup.sh
bash WatchWolfSetup.sh --build --dev --threads 4
bash WatchWolfSetup.sh --run
```

The initial build downloads server jars and container images, requires at least 1.5 GB of free space, and can take up to an hour. Wait until both the Servers Manager on port `8000` and Clients Manager on port `7000` are ready.

## Running tests

From the project root on Windows:

```powershell
.\gradlew.bat testWatchWolf
```

From WSL or Linux:

```bash
./gradlew testWatchWolf
```

The task builds the shaded plugin, copies it to `build/watchwolf/chunkspawnerlimiter.jar`, then provisions the servers and clients declared in `src/testWatchWolf/resources/watchwolf.yaml`.

The real-server suite verifies that:

- Paper loads ChunkSpawnerLimiter and exposes its version command.
- A grouped block limit is enforced independently per chunk.
- Breaking a limited block releases capacity for another placement.
- A grouped entity limit is enforced independently per chunk.
- Entity death releases capacity for a replacement entity.
- Players remain outside entity enforcement when player killing is disabled.

The deterministic plugin configuration used by these tests is copied from `src/testWatchWolf/resources/csl/config.yml` into the server's `plugins/ChunkSpawnerLimiter` directory before startup.

Change `provider` in that file when WatchWolf runs on another host. Add server implementations or versions to `server-type` only after confirming their jars exist under the WatchWolf Servers Manager's `server-types` directory.

## Version pin

The project pins WatchWolf Tester to dev commit `66cebe92b2`. The stable Tester branch still exposes the older 2023 client, while the pinned dev revision uses WatchWolf Core `0.3.3` and matches the actively developed protocol.
