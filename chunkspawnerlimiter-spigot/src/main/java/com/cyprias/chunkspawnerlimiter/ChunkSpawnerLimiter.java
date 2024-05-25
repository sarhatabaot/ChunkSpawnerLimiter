package com.cyprias.chunkspawnerlimiter;

import co.aikar.commands.PaperCommandManager;
import com.cyprias.chunkspawnerlimiter.commands.CslCommand;
import com.cyprias.chunkspawnerlimiter.configs.BlocksConfig;
import com.cyprias.chunkspawnerlimiter.configs.CslConfig;
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
	private CslConfig cslConfig;

	private BlocksConfig blocksConfig;

	private Metrics metrics;

	@Override
	public void onEnable() {
		initConfigs();
		ChatUtil.init(this);

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
	}

	private void registerListeners() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new EntityListener(this), this);
		pm.registerEvents(new WorldListener(this), this);
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
}
