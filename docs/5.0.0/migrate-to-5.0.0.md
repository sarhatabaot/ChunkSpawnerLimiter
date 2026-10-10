---
icon: rotate
---

# Migrate to 5.0.0

## Migration Guide (Old → New Config)

This guide explains how to convert your old CSL configuration to the new structured format.

The new config is cleaner, grouped logically, and more flexible — but some paths have changed.

***

## 🧭 Overview of Changes

| Old Config                     | New Config                                  |
| ------------------------------ | ------------------------------------------- |
| `debug`                        | `debug-messages`                            |
| `watch-creature-spawn`         | `events.spawn.creature`                     |
| `watch-vehicle-create-event`   | `events.spawn.vehicle`                      |
| `watch-entity-spawns`          | `events.spawn.entity`                       |
| `active-inspections`           | `events.inspections.enabled`                |
| `inspection-frequency`         | `events.inspections.frequency`              |
| `preserve-named-entities`      | `entities.preservation.named-entities`      |
| `preserve-raid-entities`       | `entities.preservation.raid-entities`       |
| `kill-instead-of-remove`       | `entities.removal.mode`                     |
| `drop-items-from-armor-stands` | `entities.removal.armor-stand.drop`         |
| `log-armor-stand-tick-warning` | `entities.removal.armor-stand.log-warnings` |
| `ignore-metadata`              | `entities.ignore.metadata`                  |
| `spawn-reasons:` (boolean map) | `spawn-reasons:` (list)                     |
| `worlds.worlds`                | `worlds.list`                               |

***

## 1. Debug Setting

#### Old

```yaml
debug: true
```

#### New

```yaml
debug-messages: true
```

***

## 2. Spawn Event Watching

#### Old

```yaml
watch-creature-spawn: true
watch-vehicle-create-event: true
watch-entity-spawns: true
```

#### New

```yaml
events:
  spawn:
    creature: true
    vehicle: true
    entity: true
```

***

## 3. Chunk Inspections

#### Old

```yaml
active-inspections: true
inspection-frequency: 60
```

#### New

```yaml
events:
  inspections:
    enabled: true
    frequency: 60
```

***

## 4. Preservation Settings

#### Old

```yaml
preserve-named-entities: true
preserve-raid-entities: true
```

#### New

```yaml
entities:
  preservation:
    named-entities: true
    raid-entities: true
```

***

## 5. Removal Mode Changes

#### Old

```yaml
kill-instead-of-remove: true
```

The old config only allowed remove or kill behavior.

#### New

```yaml
entities:
  removal:
    mode: "enforce"
```

### Mode Conversion Guide

| Old Behavior            | New Mode         |
| ----------------------- | ---------------- |
| Remove excess           | `"remove"`       |
| Kill excess             | `"kill"`         |
| Remove + actively check | `"enforce"`      |
| Kill + actively check   | `"enforce-kill"` |

If you previously had:

```yaml
kill-instead-of-remove: true
```

Use:

```yaml
mode: "kill"
```

or

```yaml
mode: "enforce-kill"
```

***

## 6. Armor Stand Settings

#### Old

```yaml
drop-items-from-armor-stands: false
log-armor-stand-tick-warning: true
```

#### New

```yaml
entities:
  removal:
    armor-stand:
      drop: false
      log-warnings: true
```

***

## 7. Ignored Metadata

#### Old

```yaml
ignore-metadata:
  - shopkeeper
```

#### New

```yaml
entities:
  ignore:
    metadata:
      - shopkeeper
```

NBT support is now grouped under the same section:

```yaml
entities:
  ignore:
    nbt:
      - "tag"
```

⚠ Requires NBTAPI installed.

***

## 8. Spawn Reasons (Major Change)

#### ❗ Old Format (Boolean Map)

```yaml
spawn-reasons:
  BUILD_IRONGOLEM: true
  NATURAL: true
  SPAWNER: false
```

#### ✅ New Format (List)

Only include the reasons you want enabled:

```yaml
spawn-reasons:
  - "BUILD_IRONGOLEM"
  - "NATURAL"
```

If it is in the list → it is checked. If it is not in the list → it is ignored.

***

## 9. World Configuration

#### Old

```yaml
worlds:
  mode: "excluded"
  worlds: ["world"]
```

#### New

```yaml
worlds:
  mode: excluded
  list: ["world"]
```

Only the key name changed from `worlds` → `list`.

***

## 10. Entity Groups

The concept remains the same, but is now clearly separated:

```yaml
entities:
  entity-groups:
    ANIMALS:
      - COW
      - SHEEP
```

Limits are defined separately:

```yaml
entities:
  limits:
    ANIMALS: 50
```

This makes group definitions cleaner and easier to manage.

***

## 11. Block Limits (New Section)

Block restrictions are now fully separated:

```yaml
blocks:
  limits:
    SPAWNER: 10
```

Block groups are also supported:

```yaml
blocks:
  block-groups:
    ores:
      - DIAMOND_BLOCK
      - GOLD_BLOCK
```

This did not exist in the old format.

***

## 🧨 Breaking Changes Summary

* Spawn reasons changed from boolean map → list
* Removal behavior expanded to multiple modes
* Event watching moved under `events`
* Preservation moved under `entities`
* Metadata & NBT filtering grouped under `entities.ignore`
* World list key renamed to `list`

***

## ✅ Recommended Migration Method

1. Stop server
2. Rename old `config.yml` to `config.yml.old`
3. Start server once to generate new config
4. Reapply your custom limits using this guide
5. Restart server

***

## 🆘 Common Migration Mistakes

#### ❌ Leaving old boolean spawn reasons format

New config requires a list.

#### ❌ Using old `kill-instead-of-remove`

This no longer exists.

#### ❌ Forgetting indentation under `entities`

YAML is indentation-sensitive.

***

## 🎉 You’re Done

Your config is now using the new structured CSL format.

