package com.cyprias.chunkspawnerlimiter;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bstats.bukkit.Metrics;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.chunkspawnerlimiter.listeners.EntityListener;
import com.cyprias.chunkspawnerlimiter.listeners.WorldListener;

public class Plugin extends JavaPlugin {
    private static Plugin instance = null;
    public static Logger logger;
    public static String chatPrefix = "&4[&bCSL&4]&r ";

    public static HashMap<String, Location> deaths = new HashMap<>();

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.getConfig().options().header(getName()+" v"+getDescription().getVersion()+" config.yml");

        // Register our event listener.
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);

        Metrics metrics = new Metrics(this);
    }

    public static void debug(String msg){
        Plugin.logger.info("[debug] "+msg);
    }

    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public static Plugin getInstance() {
        return instance;
    }


    public static int scheduleSyncRepeatingTask(Runnable run, long delay) {
        return scheduleSyncRepeatingTask(run, delay, delay);
    }

    public static int scheduleSyncRepeatingTask(Runnable run, long start, long delay) {
        return instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, run, start, delay);
    }

    public static void cancelTask(int taskID) {
        instance.getServer().getScheduler().cancelTask(taskID);
    }


}
