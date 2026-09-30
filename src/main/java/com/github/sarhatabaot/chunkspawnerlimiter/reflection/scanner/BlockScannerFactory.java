package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.BukkitBlockScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.LegacyNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.ModernNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.SpigotNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.SnapshotBlockScanner;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Factory for creating bounded snapshot scanners for runtime use and explicit
 * version-specific scanners for compatibility tests.
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

    /**
     * Create a BlockScanner with explicit scanner type override.
     * Useful for testing or forcing a specific implementation.
     * 
     * @param plugin the plugin instance
     * @param config the plugin configuration
     * @param counterManager the counter data manager
     * @param scannerType the specific scanner type to use
     * @return the requested BlockScanner implementation
     */
    public static BlockScanner createSpecific(Plugin plugin, PluginConfig config, CounterDataManager counterManager, ScannerType scannerType) {
        BlockScanner scanner = switch (scannerType) {
            case MODERN_NMS -> new ModernNmsScanner(plugin, config, counterManager);
            case SPIGOT_NMS -> new SpigotNmsScanner(plugin, config, counterManager);
            case LEGACY_NMS -> new LegacyNmsScanner(plugin, config, counterManager);
            case BUKKIT -> new BukkitBlockScanner(plugin, config, counterManager);
        };

        if (!scanner.isSupported()) {
            Bukkit.getLogger().log(Level.WARNING, "[BlockScannerFactory] Requested scanner " + scannerType + 
                    " is not supported, falling back to Bukkit");
            return new BukkitBlockScanner(plugin, config, counterManager);
        }

        Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using explicitly requested " + scanner.getImplementationName());
        return scanner;
    }

    /**
     * Enumeration of available scanner types.
     */
    public enum ScannerType {
        MODERN_NMS,
        SPIGOT_NMS,
        LEGACY_NMS,
        BUKKIT
    }
}
