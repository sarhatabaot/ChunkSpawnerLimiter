package com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import com.github.sarhatabaot.chunkspawnerlimiter.counter.CounterDataManager;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.BukkitBlockScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.LegacyNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.ModernNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.impl.SpigotNmsScanner;
import com.github.sarhatabaot.chunkspawnerlimiter.reflection.scanner.util.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Factory for creating the appropriate BlockScanner implementation based on the server version.
 * Attempts version-specific NMS scanners before falling back to the universal Bukkit scanner.
 */
public class BlockScannerFactory {

    /**
     * Create a BlockScanner instance optimized for the current server version.
     * 
     * @param plugin the plugin instance
     * @param config the plugin configuration
     * @param counterManager the counter data manager
     * @return the best available BlockScanner implementation
     */
    public static BlockScanner create(Plugin plugin, PluginConfig config, CounterDataManager counterManager) {
        MinecraftVersion version = MinecraftVersion.detect();
        
        Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Detected Minecraft version: " + version);

        // Try version-specific scanners in order of preference
        
        // 1. Modern NMS (1.17+)
        if (version.isModern()) {
            BlockScanner scanner = new ModernNmsScanner(plugin, config, counterManager);
            if (scanner.isSupported()) {
                Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using " + scanner.getImplementationName());
                return scanner;
            }
            Bukkit.getLogger().log(Level.WARNING, "[BlockScannerFactory] ModernNMS not supported, trying alternatives...");
        }

        // 2. Spigot NMS (1.13-1.16)
        if (version.isSpigot()) {
            BlockScanner scanner = new SpigotNmsScanner(plugin, config, counterManager);
            if (scanner.isSupported()) {
                Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using " + scanner.getImplementationName());
                return scanner;
            }
            Bukkit.getLogger().log(Level.WARNING, "[BlockScannerFactory] SpigotNMS not supported, trying alternatives...");
        }

        // 3. Legacy NMS (1.8.8-1.12)
        if (version.isLegacy()) {
            BlockScanner scanner = new LegacyNmsScanner(plugin, config, counterManager);
            if (scanner.isSupported()) {
                Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using " + scanner.getImplementationName());
                return scanner;
            }
            Bukkit.getLogger().log(Level.WARNING, "[BlockScannerFactory] LegacyNMS not supported, trying alternatives...");
        }

        // 4. Fallback: Pure Bukkit API (always works)
        Bukkit.getLogger().log(Level.WARNING, "[BlockScannerFactory] NMS reflection failed for version " + version + 
                ", falling back to Bukkit API (slower performance)");
        BlockScanner fallback = new BukkitBlockScanner(plugin, config, counterManager);
        Bukkit.getLogger().log(Level.INFO, "[BlockScannerFactory] Using " + fallback.getImplementationName());
        return fallback;
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
