package com.cyprias.chunkspawnerlimiter.utils;

import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.configs.impl.CslConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * ChatUtil
 */
public class ChatUtil {
    private static CslConfig config;
    private static ChunkSpawnerLimiter plugin;

    private ChatUtil() {
        throw new UnsupportedOperationException("Util class.");
    }

    public static void init(final @NotNull ChunkSpawnerLimiter plugin) {
        ChatUtil.config = plugin.getCslConfig();
        ChatUtil.plugin = plugin;
    }

    public static void message(@NotNull CommandSender target, String message) {
        target.sendMessage(colorize(message));
    }

    public static void message(@NotNull CommandSender target, String message, Object... args) {
        target.sendMessage(String.format(colorize(message), args));
    }

    public static void title(@NotNull Player player, final String title, final String subtitle, String material, int amount) {

        player.sendTitle(
                replace(colorize(title), material, amount),
                replace(colorize(subtitle), material, amount),
                10,
                70,
                20
        );
    }

    private static @NotNull String replace(@NotNull String message, String material, int amount) {
        return message.replace("{material}", material)
                .replace("{amount}", String.valueOf(amount));
    }

    @Contract("_ -> new")
    public static @NotNull String colorize(String message) {
        if (message == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void debug(String message) {
        if (config.isDebugMessages()) {
            plugin.getLogger().info(() -> "DEBUG " + message);
        }
    }

    public static void debug(String message, Object... args) {
        if (config.isDebugMessages()) {
            plugin.getLogger().info(() -> "DEBUG " + String.format(message, args));
        }
    }

    public static void logAndCheckArmorStandTickWarning() {
        if (Util.isArmorStandTickDisabled()) {
            logArmorStandTickWarning();
        }
    }

    public static void logArmorStandTickWarning() {
        if (config.isLogArmorStandTickWarning()) {
            Bukkit.getLogger().warning(() -> "[CSL] Armor Stand Ticks are disabled in your paper-world.yml or paper-world-defaults.yml file");
            Bukkit.getLogger().warning(() -> "[CSL] The kill instead of remove feature will not work properly with this setting enabled.");
            Bukkit.getLogger().warning(() -> "[CSL] Instead, the armor stand will be removed normally instead of \"killed\"");
        }
    }


}
