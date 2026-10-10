# blocks.yml

{% hint style="warning" %}
By default, this feature is disabled. Enable is by setting `enabled` to `true`
{% endhint %}

### Blocks

You can limit any block in a chunk as long as it's from [https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)

For example:

```yaml
blocks:
  SPAWNER: 10
```

### Notifications

You can modify which notifications players receive if a limit has been reached.

```yaml
notify:
  title: true
  message: false
```

{% hint style="info" %}
Want to modify the messages & titles themselves? See [#messages](config.yml.md#messages "mention")
{% endhint %}

### World Settings (count)

You can modify the default y values being checked by the plugin. Anything between these 2 values will be checked. This is useful if you aren't running the latest server version, modified your world height, or just want to modify these values for some reason.

```yaml
count:
  min-y: -64
  max-y: 256
```

#### Specific World Settings

You can specify specific worlds as well. For example:

<pre class="language-yaml"><code class="lang-yaml"><strong>world_nether:
</strong>  min-y: 0
  max-y: 128
</code></pre>
