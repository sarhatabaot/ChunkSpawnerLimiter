package com.cyprias.chunkspawnerlimiter.configs;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CslConfig extends ConfigFile<ChunkSpawnerLimiter>{

	private boolean metrics;
	/* Properties */
	private boolean debugMessages;
	private boolean checkChunkLoad;
	private boolean checkChunkUnload;
	private boolean activeInspections;
	private boolean watchCreatureSpawns;
	private boolean watchVehicleCreate;
	private int checkSurroundingChunks;
	private int inspectionFrequency;
	private boolean notifyPlayers;
	private boolean preserveNamedEntities;
	private boolean preserveRaidEntities;
	private List<String> ignoreMetadata;
	private boolean killInsteadOfRemove;

	/* Messages */
	private String removedEntities;
	private String reloadedConfig;

	private String maxAmountBlocks;
	private String maxAmountBlocksTitle;
	private String maxAmountBlocksSubtitle;

	private List<String> excludedWorlds;



	public CslConfig(final @NotNull ChunkSpawnerLimiter plugin) {
		super(plugin, "", "config.yml", "");
		saveDefaultConfig();
	}

	@Override
	public void initValues() {
		this.excludedWorlds = config.getStringList("excluded-worlds");
		String propertiesPath = "properties.";
		this.debugMessages =config.getBoolean(propertiesPath + "debug-messages");
		this.checkChunkLoad = config.getBoolean(propertiesPath + "check-chunk-load");
		this.checkChunkUnload = config.getBoolean(propertiesPath + "check-chunk-unload");
		this.activeInspections = config.getBoolean(propertiesPath + "active-inspections");
		this.watchCreatureSpawns =config.getBoolean(propertiesPath + "watch-creature-spawns");
		this.watchVehicleCreate =config.getBoolean(propertiesPath + "watch-vehicle-spawns");
		this.checkSurroundingChunks =config.getInt(propertiesPath + "check-surrounding-chunks");
		this.inspectionFrequency = config.getInt(propertiesPath + "inspection-frequency", 300);
		this.notifyPlayers = config.getBoolean(propertiesPath + "notify-players", false);
		this.preserveNamedEntities = config.getBoolean(propertiesPath + "preserve-named-entities", true);
		this.preserveRaidEntities = config.getBoolean(propertiesPath + "preserve-raid-entities", true);
		this.ignoreMetadata = config.getStringList(propertiesPath + "ignore-metadata");
		this.killInsteadOfRemove = config.getBoolean(propertiesPath + "kill-instead-of-remove", false);

		String messagesPath = "messages.";
		this.removedEntities = config.getString(messagesPath + "removedEntities");
		this.reloadedConfig = config.getString(messagesPath + "reloadedConfig", "&cReloaded csl config.");
		this.maxAmountBlocks = config.getString(messagesPath + "maxAmountBlocks", "&6Cannot place more &4{material}&6. Max amount per chunk &2{amount}.");
		this.maxAmountBlocksTitle =config.getString(messagesPath + "maxAmountBlocksTitle", "&6Cannot place more &4{material}&6.");
		this.maxAmountBlocksSubtitle = config.getString(messagesPath + "maxAmountBlocksSubtitle", "&6Max amount per chunk &2{amount}.");
		this.metrics = config.getBoolean("metrics", true);
	}

	public boolean metrics() {
		return metrics;
	}

	public int getEntityLimit(String entityType) {
		return config.getInt("entities." + entityType);
	}

	public boolean isSpawnReason(String reason) {
		return config.getBoolean("spawn-reasons." + reason);
	}

	public boolean contains(String property) {
		return config.contains(property);
	}

	public ConfigurationSection getSpawnReasons() {
		return config.getConfigurationSection("spawn-reasons");
	}

	public ConfigurationSection getEntityLimits() {
		return config.getConfigurationSection("entities");
	}

	public List<String> getExcludedWorlds() {
		return excludedWorlds;
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
}
