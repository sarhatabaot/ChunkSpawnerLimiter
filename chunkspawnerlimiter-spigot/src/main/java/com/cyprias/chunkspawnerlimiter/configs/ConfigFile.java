package com.cyprias.chunkspawnerlimiter.configs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * @author sarhatabaot
 * Extracted from KrakenCore, since it only supports JDK 16+
 */
public abstract class ConfigFile<T extends JavaPlugin> {
    private final String resourcePath;

    protected final T plugin;
    protected final String fileName;
    protected final File folder;

    protected File file;
    protected FileConfiguration config;

    protected ConfigFile(final @NotNull T plugin, final String resourcePath, final String fileName, final String folder) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.resourcePath = resourcePath;
        this.folder = new File(plugin.getDataFolder().getPath() + File.separator + folder);
    }

    public void saveDefaultConfig() {
        if (this.file == null) {
            this.file = new File(folder, fileName);
        }

        if (!this.file.exists()) {
            plugin.saveResource(resourcePath + fileName, false);
        }

        reloadConfig();
    }

    public void saveConfig() {
        if (this.config == null)
            return;

        if (file == null)
            return;

        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning(ex.getMessage());
        }
    }


    public void reloadConfig() {
        if (file == null) {
            file = new File(folder, fileName);
        }

        config = YamlConfiguration.loadConfiguration(file);
        initValues();
    }

    public void reloadDefaultConfig() {
        if (file == null) {
            file = new File(folder, fileName);
        }

        if (!file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
            try (InputStream resource = plugin.getResource(resourcePath + fileName)) {
                if (resource != null) {
                    try (Reader defConfigStream = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                        config.setDefaults(defConfig);
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }

    @NotNull
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return this.config;
    }

    public abstract void initValues();
}