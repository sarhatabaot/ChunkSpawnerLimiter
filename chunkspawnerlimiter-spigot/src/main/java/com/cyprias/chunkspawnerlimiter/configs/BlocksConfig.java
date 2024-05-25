package com.cyprias.chunkspawnerlimiter.configs;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author sarhatabaot
 */
public class BlocksConfig extends ConfigFile<ChunkSpawnerLimiter> {
    private boolean enabled;
    private Map<Material, Integer> materialLimits;
    private boolean notifyMessage;
    private boolean notifyTitle;

    private int minY;
    private int maxY;

    public BlocksConfig(final @NotNull ChunkSpawnerLimiter plugin) {
        super(plugin, "", "blocks.yml", "");
        saveDefaultConfig();
    }

    @Override
    public void initValues() {
        this.enabled = config.getBoolean("enabled", false);
        this.materialLimits = convertToMaterialLimits(config.getConfigurationSection("blocks").getValues(false));
        this.notifyMessage = config.getBoolean("notify.message", false);
        this.notifyTitle = config.getBoolean("notify.title", true);
        this.minY = config.getInt("count.min-y", -64);
        this.maxY = config.getInt("count.max-y", 256);
    }

    public Map<Material, Integer> getMaterialLimits() {
        return materialLimits;
    }

    public Integer getLimit(final Material material) {
        return materialLimits.get(material);
    }

    public boolean hasLimit(final Material material) {
        return materialLimits.containsKey(material);
    }


    private @NotNull Map<Material, Integer> convertToMaterialLimits(@NotNull Map<String, Object> map) {
        final Map<Material, Integer> materialIntegerEnumMap = new EnumMap<>(Material.class);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Material material = Material.getMaterial(entry.getKey());

            if (material == null) {
                plugin.getLogger().warning("Incorrect material name, check your blocks.yml and make sure it's set exactly.");
                plugin.getLogger().warning("Skipping entry (material=" + entry.getKey() + ")");
                continue;
            }

            final Integer limit = (Integer) entry.getValue();
            if (limit == null) {
                plugin.getLogger().warning("Missing limit value for material = " + material.name() + ", skipping entry.");
                continue;
            }
            materialIntegerEnumMap.put(material, limit);
        }

        return materialIntegerEnumMap;
    }

    public boolean isNotifyMessage() {
        return notifyMessage;
    }

    public boolean isNotifyTitle() {
        return notifyTitle;
    }

    public int getMinY() {
        return minY;
    }

    @Contract(pure = true)
    private @NotNull String getWorldPath(final String worldName) {
        return "count." + worldName;
    }

    public boolean hasWorld(final String worldName) {
        return config.contains(getWorldPath(worldName));
    }

    public int getMinY(final String worldName) {
        return config.getInt(getWorldPath(worldName) + ".min-y", getMinY());
    }

    public int getMaxY(final String worldName) {
        return config.getInt(getWorldPath(worldName) + ".max-y", getMaxY());
    }

    public int getMaxY() {
        return maxY;
    }

    public boolean isEnabled() {
        return enabled;
    }


}
