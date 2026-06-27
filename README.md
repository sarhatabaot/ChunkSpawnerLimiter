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

A plugin for enforcing spawner limits per chunk in Minecraft,
designed to improve server performance by controlling mob spawns.
This plugin is especially useful for large servers or resource-constrained environments.

## Features

* Spawner Limits: Enforce per-chunk spawner limits.
* Customizable: Adjust limits via configuration files.
* Lightweight: Minimal performance impact on your server.
* Support for Multiple Versions: Works with Minecraft 1.8.8-1.21.10.

## Release Notes (5.0.0)

### Known Limitations

- **Async chunk scanning**: chunk block scans currently run asynchronously for performance. Some server implementations may treat Bukkit world access off the main thread as unsafe. If you encounter instability, consider disabling async scans or switching to synchronous scanning until a thread-safe scan path is introduced.
