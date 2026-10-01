# WatchWolf with Docker Compose

This Compose setup replaces WatchWolf's upstream `--install` and `--run` lifecycle with `docker compose`.
It intentionally preserves the upstream build/bootstrap process because the current ServersManager dynamically
creates Minecraft child containers and expects its `server-types` JAR cache.

## Start

```bash
./bootstrap.sh
docker compose up -d --build
docker compose logs -f
```

## Stop

```bash
docker compose down
```

Minecraft child containers created by ServersManager are separate Docker containers and are not Compose services;
that matches WatchWolf's current architecture.

## Why host networking?

The current upstream `run.sh` uses `--network host` for both managers. Port mappings are therefore intentionally
not declared in this Compose file.

## itzg/minecraft-server integration

`itzg/minecraft-server` can replace WatchWolf's pre-downloaded Paper/Spigot JAR cache only after changing the
ServersManager child-container launch implementation. The desired mapping is conceptually:

- child image: `itzg/minecraft-server:stable` (or a pinned release tag)
- `EULA=TRUE`
- Paper: `TYPE=PAPER`
- requested Minecraft version: `VERSION=<WatchWolf requested version>`
- per-instance persistent directory mounted at `/data`
- WatchWolf and usual plugin JARs made available under `/data/plugins` (or `/plugins`)
- WatchWolf's CPU/memory limits translated to Docker resource limits and/or `MEMORY`/`MAX_MEMORY`
- published Minecraft port kept identical to the port allocated by WatchWolf

Once ServersManager does that, the expensive Spigot BuildTools/Paper pre-download loop can be removed from bootstrap.
