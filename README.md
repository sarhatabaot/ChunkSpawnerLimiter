> [!NOTE]
> **Project Status: Slowing Down**
>
> This project isn't going anywhere, but I'm gradually shifting it towards **maintenance mode**.
>
> I'll continue fixing bugs, updating compatibility when needed, and reviewing pull requests, but I want to be realistic about my available time. New features will be much less frequent than they have been in the past.
>
> Thanks to everyone who's used the plugin, reported issues, suggested ideas, or contributed. I really appreciate it, and I hope to keep this project useful for a long time, even if development slows down.

# ChunkSpawnerLimiter

[![Codacy Badge](https://img.shields.io/codacy/grade/d3eefde107a2471d856542804c4a3016?logo=codacy&style=for-the-badge)](https://app.codacy.com/gh/sarhatabaot/ChunkSpawnerLimiter/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Java CI](https://img.shields.io/github/actions/workflow/status/sarhatabaot/chunkspawnerlimiter/gradle.yml?branch=master&logo=github&style=for-the-badge)](https://github.com/sarhatabaot/ChunkSpawnerLimiter/actions/workflows/gradle.yml)
[![GitBook Badge](https://img.shields.io/badge/wiki-BBDDE5?logo=gitbook&logoColor=000&style=for-the-badge)](https://csl.sarhatabaot.net/)

Maintained fork of the [ChunkSpawnerLimiter](https://dev.bukkit.org/projects/chunkspawnerlimiter) plugin.

A Minecraft plugin for limiting configured entity types and block materials per chunk.
It checks new spawns and placements, and can clean up excess existing entities when chunks load or the configuration reloads.

## Features

* Configure per-chunk limits for entity types, block materials, or groups of either.
* Choose whether to prevent new violations, remove excess entities, or kill them.
* Recheck loaded chunks periodically when inspections are enabled.
* Target Minecraft versions from 1.8.8 through 1.21.10; the build requires Java 21.

## Configuration

Edit `plugins/ChunkSpawnerLimiter/config.yml` and run `/csl reload` to apply changes.
The default configuration documents the available limits and removal modes.

Block scans capture a chunk snapshot on the server thread and count its blocks asynchronously.
There is no configuration switch for synchronous scanning.

## Testing

Run `./gradlew check` for the unit and legacy test suites. Real Paper integration tests use WatchWolf; see [WATCHWOLF.md](WATCHWOLF.md) for setup and commands.
