package com.cyprias.chunkspawnerlimiter.configs;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CslConfig extends ConfigFile<ChunkSpawnerLimiter> {
    private final Logger logger = Logger.getLogger(CslConfig.class.getName());
    private Map<String, Integer> entityLimits;
    private Set<String> spawnReasons;

    private boolean metrics;
    /* Properties */
    private boolean debugMessages;
    private boolean checkChunkLoad;
    private boolean checkChunkUnload;
    private boolean activeInspections;
    private boolean watchCreatureSpawns;
    private boolean watchVehicleCreate;
    private boolean watchEntitySpawns;
    private int checkSurroundingChunks;
    private int inspectionFrequency;
    private boolean notifyPlayers;
    private boolean preserveNamedEntities;
    private boolean preserveRaidEntities;
    private List<String> ignoreMetadata;
    private boolean killInsteadOfRemove;
    private boolean dropItemsFromArmorStands;
    private boolean logArmorStandTickWarning;

    /* Worlds */
    private List<String> worldsNames;
    private WorldsMode worldsMode;

    /* Messages */
    private String removedEntities;
    private String reloadedConfig;

    private String maxAmountBlocks;
    private String maxAmountBlocksTitle;
    private String maxAmountBlocksSubtitle;

    public CslConfig(final @NotNull ChunkSpawnerLimiter plugin) {
        super(plugin, "", "config.yml", "");
        saveDefaultConfig();
    }

    @Override
    public void initValues() {
        final ConfigurationSection propertiesSection = config.getConfigurationSection("properties");

        if (propertiesSection == null) {
            throw new NullPointerException("Your properties section is missing! Disabling plugin.");
        }

        this.debugMessages = propertiesSection.getBoolean("debug-messages", false);
        this.checkChunkLoad = propertiesSection.getBoolean("check-chunk-load", false);
        this.checkChunkUnload = propertiesSection.getBoolean("check-chunk-unload", false);
        this.activeInspections = propertiesSection.getBoolean("active-inspections", true);
        this.watchCreatureSpawns = propertiesSection.getBoolean("watch-creature-spawns", true);
        this.watchVehicleCreate = propertiesSection.getBoolean("watch-vehicle-create-event", true);
        this.watchEntitySpawns = propertiesSection.getBoolean("watch-entity-spawns", true);
        this.checkSurroundingChunks = propertiesSection.getInt("check-surrounding-chunks", 1);
        this.inspectionFrequency = propertiesSection.getInt("inspection-frequency", 300);
        this.notifyPlayers = propertiesSection.getBoolean("notify-players", false);
        this.preserveNamedEntities = propertiesSection.getBoolean("preserve-named-entities", true);
        this.preserveRaidEntities = propertiesSection.getBoolean("preserve-raid-entities", true);
        this.ignoreMetadata = propertiesSection.getStringList("ignore-metadata");
        this.killInsteadOfRemove = propertiesSection.getBoolean("kill-instead-of-remove", false);
        this.dropItemsFromArmorStands = propertiesSection.getBoolean("drop-items-from-armor-stands", false);
        this.logArmorStandTickWarning = propertiesSection.getBoolean("log-armor-stand-tick-warning", true);

        this.worldsNames = config.getStringList("worlds.worlds");
        this.worldsMode = initWorldsMode();

        String messagesPath = "messages.";
        this.removedEntities = config.getString(messagesPath + "removedEntities");
        this.reloadedConfig = config.getString(messagesPath + "reloadedConfig", "&cReloaded csl config.");
        this.maxAmountBlocks = config.getString(messagesPath + "maxAmountBlocks", "&6Cannot place more &4{material}&6. Max amount per chunk &2{amount}.");
        this.maxAmountBlocksTitle = config.getString(messagesPath + "maxAmountBlocksTitle", "&6Cannot place more &4{material}&6.");
        this.maxAmountBlocksSubtitle = config.getString(messagesPath + "maxAmountBlocksSubtitle", "&6Max amount per chunk &2{amount}.");
        this.metrics = config.getBoolean("metrics", true);

        this.entityLimits = loadEntityLimits();
        this.spawnReasons = loadSpawnReasons();
    }

    public boolean metrics() {
        return metrics;
    }

    private Set<String> loadSpawnReasons() {
        final ConfigurationSection spawnReasonsSection = config.getConfigurationSection("spawn-reasons");
        if (spawnReasonsSection == null) {
            logger.warning("Spawn reasons section is missing. Returning an empty set.");
            return Collections.emptySet();
        }

        return spawnReasonsSection.getValues(false).entrySet().stream()
                .filter(entry -> {
                    if (!(entry.getValue() instanceof Boolean)) {
                        logger.warning("Spawn reason '" + entry.getKey() + "' has an invalid value (" + entry.getValue() + "). Expected Boolean. Skipping.");
                        return false;
                    }
                    return (Boolean) entry.getValue();
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }


    private Map<String, Integer> loadEntityLimits() {
        final ConfigurationSection entitySection = config.getConfigurationSection("entities");
        if (entitySection == null) {
            logger.warning("Entity limits section is missing. Returning an empty map.");
            return Collections.emptyMap();
        }

        return entitySection.getValues(false).entrySet().stream()
                .filter(entry -> {
                    if (entry.getValue() == null) {
                        logger.warning("Entity limit for '" + entry.getKey() + "' is null. Skipping.");
                        return false;
                    }
                    if (!(entry.getValue() instanceof Integer)) {
                        logger.warning("Entity limit for '" + entry.getKey() + "' is not an integer (" + entry.getValue() + "). Skipping.");
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Integer) entry.getValue()));
    }


    public int getEntityLimit(String entityType) {
        return entityLimits.get(entityType);
    }

    public boolean isSpawnReason(String reason) {
        return spawnReasons.contains(reason);
    }

    public boolean contains(String property) {
        return config.contains(property);
    }

    public String getFormattedSpawnReasons() {
        return StringUtils.join(spawnReasons, ", ");
    }

    public String getFormattedEntityLimits() {
        return entityLimits.toString();
    }

    public boolean isWorldAllowed(final String worldName) {
        final List<String> worldNames = getWorldNames();
        if (getWorldsMode() == WorldsMode.EXCLUDED) {
            return !worldNames.contains(worldName);
        }
        // INCLUDED
        return worldNames.contains(worldName);
    }

    public boolean isWorldNotAllowed(final String worldName) {
        return !isWorldAllowed(worldName);
    }

    public List<String> getWorldNames() {
        return worldsNames;
    }

    private WorldsMode initWorldsMode() {
        final String mode = config.getString("worlds.mode", "excluded");
        if (mode == null) {
            return WorldsMode.EXCLUDED;
        }

        return WorldsMode.valueOf(mode.toUpperCase());
    }

    public WorldsMode getWorldsMode() {
        return worldsMode;
    }


    public boolean isDebugMessages() {
        return debugMessages;
    }

    public boolean isCheckChunkLoad() {
        return checkChunkLoad;
    }

    public boolean isCheckChunkUnload() {
        return checkChunkUnload;
    }

    public boolean isActiveInspections() {
        return activeInspections;
    }

    public boolean isWatchCreatureSpawns() {
        return watchCreatureSpawns;
    }

    public boolean isWatchVehicleCreate() {
        return watchVehicleCreate;
    }

    public int getCheckSurroundingChunks() {
        return checkSurroundingChunks;
    }

    public int getInspectionFrequency() {
        return inspectionFrequency;
    }

    public boolean isNotifyPlayers() {
        return notifyPlayers;
    }

    public boolean isPreserveNamedEntities() {
        return preserveNamedEntities;
    }

    public boolean isPreserveRaidEntities() {
        return preserveRaidEntities;
    }

    public List<String> getIgnoreMetadata() {
        return ignoreMetadata;
    }

    public boolean isKillInsteadOfRemove() {
        return killInsteadOfRemove;
    }

    public String getRemovedEntities() {
        return removedEntities;
    }

    public String getReloadedConfig() {
        return reloadedConfig;
    }

    public String getMaxAmountBlocks() {
        return maxAmountBlocks;
    }

    public String getMaxAmountBlocksTitle() {
        return maxAmountBlocksTitle;
    }

    public String getMaxAmountBlocksSubtitle() {
        return maxAmountBlocksSubtitle;
    }

    public boolean isDropItemsFromArmorStands() {
        return dropItemsFromArmorStands;
    }

    public boolean isLogArmorStandTickWarning() {
        return logArmorStandTickWarning;
    }

    public boolean isWatchEntitySpawns() {
        return watchEntitySpawns;
    }

    public enum WorldsMode {
        INCLUDED,
        EXCLUDED
    }
}
