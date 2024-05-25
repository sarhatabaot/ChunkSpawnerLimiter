package com.cyprias.chunkspawnerlimiter.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.configs.CslConfig;
import com.cyprias.chunkspawnerlimiter.messages.Command;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@CommandAlias("csl")
public class CslCommand extends BaseCommand {
    private final ChunkSpawnerLimiter plugin;

    public CslCommand(final ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
    }

    @Subcommand(Command.Reload.COMMAND)
    @CommandAlias(Command.Reload.ALIAS)
    @CommandPermission(Command.Reload.PERMISSION)
    @Description(Command.Reload.DESCRIPTION)
    public void onReload(final CommandSender sender){
        plugin.reloadConfigs();
        plugin.initMetrics();
        ChatUtil.message(sender, plugin.getCslConfig().getReloadedConfig());
    }


    @Subcommand(Command.Settings.COMMAND)
    @CommandAlias(Command.Settings.ALIAS)
    @CommandPermission(Command.Settings.PERMISSION)
    @Description(Command.Settings.DESCRIPTION)
    public void onSettings(final CommandSender sender) {
        ChatUtil.message(sender, "&2&l-- ChunkSpawnerLimiter v%s --",plugin.getDescription().getVersion());
        ChatUtil.message(sender,"&2&l-- Properties --");
        ChatUtil.message(sender,"Debug Message: %s", plugin.getCslConfig().isDebugMessages());
        ChatUtil.message(sender,"Check Chunk Load: %s", plugin.getCslConfig().isCheckChunkLoad());
        ChatUtil.message(sender,"Check Chunk Unload: %s", plugin.getCslConfig().isCheckChunkUnload());
        ChatUtil.message(sender,"Active Inspection: %s", plugin.getCslConfig().isActiveInspections());
        ChatUtil.message(sender,"Watch Creature Spawns: %s", plugin.getCslConfig().isWatchCreatureSpawns());
        ChatUtil.message(sender,"Check Surrounding Chunks: %s", plugin.getCslConfig().getCheckSurroundingChunks());
        ChatUtil.message(sender,"Inspection Frequency: %d", plugin.getCslConfig().getInspectionFrequency());
        ChatUtil.message(sender,"Notify Players: %s",plugin.getCslConfig().isNotifyPlayers());
        ChatUtil.message(sender,"Preserve Named Entities: %s", plugin.getCslConfig().isPreserveNamedEntities());
        ChatUtil.message(sender,"Ignore Metadata: %s", plugin.getCslConfig().getIgnoreMetadata().toString());
        ChatUtil.message(sender,"Excluded Worlds: %s", plugin.getCslConfig().getExcludedWorlds());
        ChatUtil.message(sender,"&2&l-- Messages --");
        ChatUtil.message(sender,"Reloaded Config: %s", plugin.getCslConfig().getReloadedConfig());
        ChatUtil.message(sender,"Removed Entities: %s", plugin.getCslConfig().getRemovedEntities());
    }

    @Subcommand(Command.Info.COMMAND)
    @CommandAlias(Command.Info.ALIAS)
    @CommandPermission(Command.Info.PERMISSION)
    @Description(Command.Info.DESCRIPTION)
    public void onInfo(final CommandSender sender) {
        ChatUtil.message(sender, "&2&l-- ChunkSpawnerLimiter v%s --",plugin.getDescription().getVersion());
        ChatUtil.message(sender,"&2&l-- Reasons to cull on: --");
        sendConfigurationSection(sender, plugin.getCslConfig().getSpawnReasons());
        ChatUtil.message(sender,"&2&l-- Entity Limits: --");
        sendConfigurationSection(sender, plugin.getCslConfig().getEntityLimits());
    }

    @HelpCommand
    public void onHelp(final CommandHelp help) {
        help.showHelp();
    }

    private void sendConfigurationSection(final CommandSender sender,final @NotNull ConfigurationSection section) {
        for(Map.Entry<String,Object> entry: section.getValues(false).entrySet()) {
            ChatUtil.message(sender,"%s: %s",entry.getKey(),entry.getValue().toString());
        }
    }
}
