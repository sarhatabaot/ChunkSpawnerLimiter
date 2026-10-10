# config.yml

### Entity Limits

{% hint style="info" %}
Use entities from [https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html)

You can use the in-game command /cslsearch to view the above list as well.
{% endhint %}



#### Groups

You can also set limits on groups. The available mobs change depending on your server's version, so keep that in mind.

<table><thead><tr><th width="150">Group</th><th>Example Mobs (1.20.4)</th></tr></thead><tbody><tr><td><code>ANIMAL</code></td><td>Chicken, Cow, MushroomCow, Ocelot, Pig, Sheep, Wolf</td></tr><tr><td><code>MONSTER</code></td><td>Blaze, CaveSpider, Creeper, Enderman, Giant, PigZombie, Silverfish, Skeleton, Spider, Witch, Wither, Zombie</td></tr><tr><td><code>AMBIENT</code></td><td>Bat</td></tr><tr><td><code>WATER_MOB</code></td><td>Squid, Fish</td></tr><tr><td><code>NPC</code></td><td>Villager</td></tr><tr><td><code>VEHICLE</code></td><td>Boat, Minecart</td></tr><tr><td><code>OTHER</code></td><td>Everything else</td></tr></tbody></table>

### Spawn Reasons

You can define what spawn reasons the plugin should check a chunk for. Simply by setting true or false.

{% hint style="info" %}
Use reasons from [https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html)
{% endhint %}

For example, if I want the chunk to be checked when an iron golem is built, I would set:

```yaml
spawn-reasons:
  BUILD_IRONGOLEM: true
```

### Properties

<table><thead><tr><th width="307">Property</th><th>Description</th></tr></thead><tbody><tr><td><code>check-chunk-load</code></td><td>Check a chunk upon load (ChunkLoadEvent).</td></tr><tr><td><code>check-chunk-unload</code></td><td>Check a chunk upon unload (ChunkUnloadEvent).</td></tr><tr><td><code>active-inspections</code></td><td><p>When a chunk is loaded, recheck it periodically.</p><p>Check-chunk-load must be true for this to work.</p></td></tr><tr><td><code>debug</code></td><td>Debug messages, will spam the console with verbose messages when set to true. Best to not set this to true unless debugging an issue.</td></tr><tr><td><code>watch-creature-spawn</code></td><td>Check a chunk when a mob spawns (CreatureSpawnEvent).</td></tr><tr><td><code>watch-vehicle-create-event</code></td><td>Check a chunk when a vehicle spawns (VehicleCreateEvent).</td></tr><tr><td><code>check-surrounding-chunks</code></td><td>Radius of surrounding chunks to check.</td></tr><tr><td><code>inspection-frequency</code></td><td>How often, in seconds, to check the chunk.</td></tr><tr><td><code>notify-players</code></td><td>Notify players in that chunk if stuff has been culled.</td></tr><tr><td><code>preserve-named-entities</code></td><td>Prioritize entities without names over older entities.</td></tr><tr><td><code>preserve-raid-entities</code></td><td>Don't remove pillagers that are part of a raid.</td></tr><tr><td><code>ignore-metadata</code></td><td>Ignore entities with any of the following metadata. For example, shopkeeper npcs.</td></tr></tbody></table>

### Include or Exclude Worlds

You can exclude  or include worlds from the check entirely by adding them to this list. And setting the mode, for example:

```yaml
worlds:
  mode: "excluded"
  worlds: ["world"]
```

#### Included

To only run the check on the listed worlds, use this configuration:

```yaml
worlds:
  mode: "included"
  worlds: ["world"]
```

#### Excluded

To disable the check on the listed worlds, use this configuration:

<pre class="language-yaml"><code class="lang-yaml"><strong>worlds:  
</strong><strong>  mode: "excluded"
</strong>  worlds: ["world"]
</code></pre>

### Messages

You can customize the messages used in the plugin by editing the messages block. Common color codes are supported.&#x20;

In some messages some internal placeholders are used: `{material}`, `{amount}`. You can remove these if you don't want to use them.

