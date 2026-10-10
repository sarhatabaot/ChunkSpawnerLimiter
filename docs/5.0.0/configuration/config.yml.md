---
metaLinks:
  alternates:
    - https://app.gitbook.com/s/CAKpWYrT1aKqaEgPKpNk/configuration/config.yml
---

# config.yml

## Main Settings

These are the global plugin settings.

```yaml
enabled: true
debug-messages: false
metrics: true
```

| Setting          | Description                                                                    |
| ---------------- | ------------------------------------------------------------------------------ |
| `enabled`        | Enables or disables the entire plugin.                                         |
| `debug-messages` | Enables verbose debug output in the console. Only enable when troubleshooting. |
| `metrics`        | Enables bStats metrics collection.                                             |

***

## Event Monitoring

Configure which events the plugin listens to and how chunk inspections work.

```yaml
events:
  spawn:
    creature: true
    vehicle: true
    entity: true
  inspections:
    enabled: true
    frequency: 60
```

### Spawn Event Watching

| Setting                 | Description                                                                   |
| ----------------------- | ----------------------------------------------------------------------------- |
| `events.spawn.creature` | Listen to `CreatureSpawnEvent`.                                               |
| `events.spawn.vehicle`  | Listen to `VehicleCreateEvent`.                                               |
| `events.spawn.entity`   | Listen to `EntitySpawnEvent`. Catches entities not handled by creature spawn. |

### Chunk Inspections

| Setting                        | Description                                  |
| ------------------------------ | -------------------------------------------- |
| `events.inspections.enabled`   | Periodically re-check loaded chunks.         |
| `events.inspections.frequency` | How often (in seconds) chunks are rechecked. |

***

## Entity Management

### Entity Groups

You can define your own entity groups.

{% hint style="info" %}
Use entities from [https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html)

You can also use the in-game command `/cslsearch` to view available entity types.
{% endhint %}

Example:

```yaml
entities:
  entity-groups:
    ANIMALS:
      - COW
      - SHEEP
      - PIG
      - CHICKEN
    MONSTERS:
      - ZOMBIE
      - SKELETON
      - CREEPER
```

You may freely add or remove entity types depending on your server version.

***

### Entity Limits

Define maximum entities per chunk per group.

```yaml
entities:
  limits:
    ANIMALS: 50
    MONSTERS: 50
    NPC: 50
    OTHER: 500
```

If a group is not defined in `entity-groups`, it will fall under `OTHER`.

***

### Preservation Settings

```yaml
entities:
  preservation:
    named-entities: true
    raid-entities: true
```

| Setting          | Description                                                 |
| ---------------- | ----------------------------------------------------------- |
| `named-entities` | Prioritizes unnamed entities for removal before named ones. |
| `raid-entities`  | Prevents pillagers involved in raids from being removed.    |

***

### Removal Modes

```yaml
entities:
  removal:
    mode: "enforce"
```

Available modes:

| Mode           | Description                                                   |
| -------------- | ------------------------------------------------------------- |
| `enforce`      | Cleans existing violations and prevents new ones.             |
| `enforce-kill` | Same as enforce, but kills entities instead of removing them. |
| `prevent`      | Only prevents new violations.                                 |
| `remove`       | Instantly removes excess entities.                            |
| `kill`         | Kills excess entities (drops + effects).                      |

#### Armor Stand Options

```yaml
entities:
  removal:
    armor-stand:
      drop: false
      log-warnings: true
```

| Setting        | Description                                  |
| -------------- | -------------------------------------------- |
| `drop`         | Drop items from armor stands when killed.    |
| `log-warnings` | Enable or disable armor stand tick warnings. |

#### Kill Players

```yaml
entities:
  removal:
    kill-players: false
```

If enabled, players exceeding chunk limits may be killed when using kill-based modes.

***

### Ignored Entities

Entities with matching metadata or NBT tags will be ignored.

```yaml
entities:
  ignore:
    metadata:
      - shopkeeper
    nbt:
      - "tag"
```

{% hint style="warning" %}
NBT filtering requires **NBTAPI** [https://modrinth.com/plugin/nbtapi](https://modrinth.com/plugin/nbtapi)
{% endhint %}

***

## Spawn Reasons

Define which spawn reasons should be processed.

{% hint style="info" %}
Use reasons from [https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html)
{% endhint %}

```yaml
spawn-reasons:
  - "BREEDING"
  - "BUILD_IRONGOLEM"
  - "SPAWNER"
```

To disable a reason, simply remove it from the list.

Example: If you want iron golems built by players to be checked, ensure:

```yaml
- "BUILD_IRONGOLEM"
```

is present in the list.

***

## Block Restrictions

### Block Groups

```yaml
blocks:
  block-groups:
    basic:
      - "STONE"
      - "DIRT"
    ores:
      - "DIAMOND_BLOCK"
      - "GOLD_BLOCK"
```

Block names must match Bukkit `Material` names.

***

### Block Limits

```yaml
blocks:
  limits:
    SPAWNER: 10
```

This example limits spawners to 10 per chunk.

***

## World Configuration

Control which worlds the plugin applies to.

```yaml
worlds:
  mode: excluded
  list: []
```

| Mode       | Description                                    |
| ---------- | ---------------------------------------------- |
| `excluded` | Plugin runs in all worlds except those listed. |
| `included` | Plugin runs only in listed worlds.             |

Example (only run in world):

```yaml
worlds:
  mode: included
  list: ["world"]
```

***

## Notifications

Configure player notifications when entities or blocks are blocked or removed.

```yaml
notifications:
  enabled: false
  cooldown-seconds: 3
  method:
    title: true
    message: false
```

| Setting            | Description                                |
| ------------------ | ------------------------------------------ |
| `enabled`          | Enable player notifications.               |
| `cooldown-seconds` | Cooldown between notifications per player. |
| `method.title`     | Send as title/subtitle.                    |
| `method.message`   | Send as chat message.                      |

***

### Custom Messages

You may use color codes with `&`.

Available placeholders:

* `{count}` — Number of entities affected
* `{type}` — Entity or block type
* `{material}` — Block material
* `{amount}` — Maximum allowed amount

Example:

```yaml
notifications:
  messages:
    entities-blocked: "&7Blocked {count} {type} from spawning in your chunk."
    entities-removed: "&7Removed {count} {type} in your chunk."
    reload-complete: "&cReloaded csl config."
    max-blocks: "&6Cannot place more &4{material}&6. Max amount per chunk &2{amount}."
    max-blocks-title: "&6Cannot place more &4{material}&6."
    max-blocks-subtitle: "&6Max amount per chunk &2{amount}."
```

You may remove placeholders if not needed.

