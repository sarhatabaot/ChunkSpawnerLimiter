package com.github.sarhatabaot.chunkspawnerlimiter.watchwolf;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class WatchWolfControlPlugin extends JavaPlugin {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (arguments.length == 0) {
            return false;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin("ChunkSpawnerLimiter");
        if (target == null) {
            sender.sendMessage("CSL test control failed: ChunkSpawnerLimiter is not loaded");
            return true;
        }

        switch (arguments[0].toLowerCase()) {
            case "reload" -> reloadTarget(sender);
            case "set" -> {
                if (arguments.length < 3) {
                    return false;
                }
                target.getConfig().set(arguments[1], parseScalar(arguments[2]));
                target.saveConfig();
                reloadTarget(sender);
            }
            case "set-list" -> {
                if (arguments.length < 2) {
                    return false;
                }
                List<String> values = arguments.length == 2 || arguments[2].isBlank()
                        ? List.of()
                        : Arrays.asList(arguments[2].split(","));
                target.getConfig().set(arguments[1], values);
                target.saveConfig();
                reloadTarget(sender);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Object parseScalar(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static void reloadTarget(CommandSender sender) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "csl reload");
        sender.sendMessage("CSL test control applied");
    }
}
