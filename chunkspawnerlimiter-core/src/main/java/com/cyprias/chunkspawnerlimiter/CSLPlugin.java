package com.cyprias.chunkspawnerlimiter;

import com.cyprias.chunkspawnerlimiter.configs.impl.CslConfig;

/**
 * This interface represents the main functionality of the Chunk Spawner Limiter plugin.
 * It provides methods for initializing metrics, configurations, registering listeners, and commands.
 */
public interface CSLPlugin {
    CslConfig getCslConfig();

    BlocksConfig getBlocksConfig();

    /**
     * Initializes the plugin's metrics system.
     * This method should set up and start the metrics collection process.
     */
    void initMetrics();

    /**
     * Initializes the plugin's configuration files.
     * This method should load and set up the default or existing configuration files.
     */
    void initConfigs();

    /**
     * Reloads the plugin's configuration files.
     * This method should update the configuration settings with the latest values from the files.
     */
    void reloadConfigs();

    /**
     * Registers the necessary event listeners for the plugin.
     * This method should add listeners for events such as player join, block break, etc.
     */
    void registerListeners();

    /**
     * Registers the plugin's commands.
     * This method should add commands that can be executed by players or administrators.
     */
    void registerCommands();
}
