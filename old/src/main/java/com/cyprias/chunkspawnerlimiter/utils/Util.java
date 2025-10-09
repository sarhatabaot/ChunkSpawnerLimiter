package com.cyprias.chunkspawnerlimiter.utils;


import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Util {
    private Util() {
        throw new UnsupportedOperationException("Util class.");
    }
    /**
     * Checks if the server software is Paper and if the configuration setting
     * for ticking armor stands is disabled.
     * <p>
     * Specifically, this method verifies two conditions:
     * <ul>
     *     <li>Whether the server is running Paper software (as opposed to Bukkit or Spigot).</li>
     *     <li>Whether the configuration option `entities.armor-stands.tick` is set to `false`
     *         in Paper's configuration file. This setting, if present, prevents armor stands from ticking.</li>
     * </ul>
     *
     * @return {@code true} if the server is running Paper and the armor stand tick option is set to {@code false},
     * {@code false} otherwise.
     */
    public static boolean isArmorStandTickDisabled() {
        // Check if the server is running Paper. If not, the setting does not apply.
        if (!isPaperServer()) {
            return false;
        }

        // Load the primary paper.yml configuration file.
        File paperConfigFile = new File(Bukkit.getServer().getWorldContainer(), "paper-world.yml");

        // If the primary file does not exist, attempt to load a default configuration file.
        if (!paperConfigFile.exists()) {
            paperConfigFile = new File("config", "paper-world-defaults.yml");
        }

        // Parse the configuration file.
        FileConfiguration paperConfig = YamlConfiguration.loadConfiguration(paperConfigFile);

        // Return the negation of the armor stand tick setting. Defaults to true if the setting is not specified.
        return !paperConfig.getBoolean("entities.armor-stands.tick", true);
    }

    public static boolean isPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
