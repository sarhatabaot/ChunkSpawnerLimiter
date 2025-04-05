package com.cyprias.chunkspawnerlimiter.configs.impl;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.configs.ConfigFile;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    private Map<String, WorldLimits> worldLimits;

    private int minLimitForCache;

    public BlocksConfig(final @NotNull ChunkSpawnerLimiter plugin) {
        super(plugin, "", "blocks.yml", "");
        saveDefaultConfig();
    }

    @Override
    public void initValues() {
        this.enabled = config.getBoolean("enabled", false);
        this.notifyMessage = config.getBoolean("notify.message", false);
        this.notifyTitle = config.getBoolean("notify.title", true);

        this.materialLimits = loadMaterialLimits();
        this.minY = config.getInt("count.default.min-y", -64);
        this.maxY = config.getInt("count.default.max-y", 256);

        this.worldLimits = loadWorldLimits();

        this.minLimitForCache = config.getInt("cache.min-limit-for-cache", 50);
    }


    private @NotNull Map<String, WorldLimits> loadWorldLimits() {
        final Map<String, WorldLimits> limits = new HashMap<>();
        final ConfigurationSection worldsSection = config.getConfigurationSection("count.worlds");

        if (worldsSection == null) {
            //This can happen and it's valid.
            return Collections.emptyMap();
        }

        final List<String> invalidEntries = new ArrayList<>();
        for (Map.Entry<String, Object> entry: worldsSection.getValues(false).entrySet()) {
            final String worldName = entry.getKey();
            final Object value = entry.getValue();
            if (!(value instanceof ConfigurationSection)) {
                invalidEntries.add("Invalid world limit configuration for: " + worldName);
                continue;
            }
            final ConfigurationSection worldSection = (ConfigurationSection) value; //might work?, might just be a map Map<String, Object>, if it works, add an instanceof check
            final int worldMinY = worldSection.getInt("min-y", this.minY);
            final int worldMaxY = worldSection.getInt("max-y", this.maxY);

            limits.put(worldName, new WorldLimits(worldName, worldMaxY, worldMinY));
        }

        if (!invalidEntries.isEmpty()) {
            plugin.getLogger().warning("Found issues in blocks.yml:");
            invalidEntries.forEach(plugin.getLogger()::warning);
            plugin.getLogger().warning("Please fix the above issues in your configuration.");
        }
        return limits;
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

    public int getMinLimitForCache() {
        return minLimitForCache;
    }

    private @NotNull Map<Material, Integer> loadMaterialLimits() {
        final ConfigurationSection blockSection = config.getConfigurationSection("blocks");
        if (blockSection == null) {
            return Collections.emptyMap();
        }

        final Map<Material, Integer> limits = new EnumMap<>(Material.class);
        final List<String> invalidEntries = new ArrayList<>();

        blockSection.getValues(false).forEach((key, value) -> {
            final Material material = Material.getMaterial(key);
            if (material == null) {
                invalidEntries.add("Invalid material name: " + key);
                return;
            }

            final int limit = blockSection.getInt(key, -1);
            if (limit < 0) {
                invalidEntries.add("Missing or invalid limit for material: " + material.name());
                return;
            }

            limits.put(material, limit);
        });

        // Log all invalid entries at once
        if (!invalidEntries.isEmpty()) {
            plugin.getLogger().warning("Found issues in blocks.yml:");
            invalidEntries.forEach(plugin.getLogger()::warning);
            plugin.getLogger().warning("Please fix the above issues in your configuration.");
        }

        return limits;
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

    public boolean hasWorld(final String worldName) {
        return worldLimits.containsKey(worldName);
    }

    public int getMinY(final String worldName) {
        return worldLimits.get(worldName).getMinY();
    }

    public int getMaxY(final String worldName) {
        return worldLimits.get(worldName).getMaxY();
    }

    public int getMaxY() {
        return maxY;
    }

    public boolean isEnabled() {
        return enabled;
    }



}
