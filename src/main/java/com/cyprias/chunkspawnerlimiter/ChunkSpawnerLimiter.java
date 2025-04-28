package com.cyprias.chunkspawnerlimiter;

import co.aikar.commands.PaperCommandManager;
import com.cyprias.chunkspawnerlimiter.commands.CslCommand;
import com.cyprias.chunkspawnerlimiter.configs.impl.BlocksConfig;
import com.cyprias.chunkspawnerlimiter.configs.impl.CslConfig;
import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityChunkInspector;
import com.cyprias.chunkspawnerlimiter.inspection.entities.EntityChunkInspectorScheduler;
import com.cyprias.chunkspawnerlimiter.listeners.EntityListener;
import com.cyprias.chunkspawnerlimiter.listeners.PlaceBlockListener;
import com.cyprias.chunkspawnerlimiter.listeners.WorldListener;
import com.cyprias.chunkspawnerlimiter.messages.Debug;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkSpawnerLimiter extends JavaPlugin {
	private EntityChunkInspectorScheduler entityChunkInspectorScheduler;
	private EntityChunkInspector entityChunkInspector;
	private CslConfig cslConfig;

	private BlocksConfig blocksConfig;

	private Metrics metrics;

	@Override
	public void onEnable() {
		initConfigs();
		ChatUtil.init(this);
		ChatUtil.logAndCheckArmorStandTickWarning();

		this.entityChunkInspector = new EntityChunkInspector(this);
		this.entityChunkInspectorScheduler = new EntityChunkInspectorScheduler(this, entityChunkInspector);
		registerListeners();
		PaperCommandManager paperCommandManager = new PaperCommandManager(this);
		paperCommandManager.enableUnstableAPI("help");
		paperCommandManager.enableUnstableAPI("brigadier");
		paperCommandManager.registerCommand(new CslCommand(this));
		initMetrics();
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	public void initMetrics() {
		if (cslConfig.metrics() && metrics == null) {
			this.metrics = new Metrics(this, 4195);
		}
	}


	private void initConfigs() {
		this.cslConfig = new CslConfig(this);
		this.blocksConfig = new BlocksConfig(this);
	}

	public void reloadConfigs() {
		this.cslConfig.reloadConfig();
		this.blocksConfig.reloadConfig();

		ChatUtil.logAndCheckArmorStandTickWarning();
	}

	private void registerListeners() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new EntityListener(cslConfig, entityChunkInspectorScheduler), this);
		pm.registerEvents(new WorldListener(this, entityChunkInspectorScheduler), this);
		pm.registerEvents(new PlaceBlockListener(this),this);
		ChatUtil.debug(Debug.REGISTER_LISTENERS);
	}

	public static void cancelTask(int taskID) {
		Bukkit.getServer().getScheduler().cancelTask(taskID);
	}

	public BlocksConfig getBlocksConfig() {
		return blocksConfig;
	}

	public CslConfig getCslConfig() {
		return cslConfig;
	}

	public EntityChunkInspector getChunkInspector() {
		return entityChunkInspector;
	}
}
