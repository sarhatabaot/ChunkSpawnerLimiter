package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.SnapshotBlockScanner;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Factory for creating bounded snapshot scanners for runtime use.
 */
public class BlockScannerFactory {

    /**
     * Create the thread-safe runtime block scanner.
     * 
     * @param plugin the plugin instance
     * @param config the plugin configuration
     * @param counterManager the counter data manager
     * @return the best available BlockScanner implementation
     */
    public static BlockScanner create(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        BlockScanner scanner = new SnapshotBlockScanner(plugin, config, counterManager);
        Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using " + scanner.getImplementationName());
        return scanner;
    }

}
