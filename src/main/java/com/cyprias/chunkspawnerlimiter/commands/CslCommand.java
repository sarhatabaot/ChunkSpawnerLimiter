package com.cyprias.chunkspawnerlimiter.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.messages.Command;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.utils.Util;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
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
    public void onReload(final CommandSender sender) {
        plugin.reloadConfigs();
        plugin.initMetrics();
        ChatUtil.message(sender, plugin.getCslConfig().getReloadedConfig());
    }


    @Subcommand(Command.Settings.COMMAND)
    @CommandAlias(Command.Settings.ALIAS)
    @CommandPermission(Command.Settings.PERMISSION)
    @Description(Command.Settings.DESCRIPTION)
    public void onSettings(final CommandSender sender) {
        ChatUtil.message(sender, "&2&l-- ChunkSpawnerLimiter v%s --", plugin.getDescription().getVersion());
        ChatUtil.message(sender, "&2&l-- Properties --");
        ChatUtil.message(sender, "Debug Message: %s", plugin.getCslConfig().isDebugMessages());
        ChatUtil.message(sender, "Check Chunk Load: %s", plugin.getCslConfig().isCheckChunkLoad());
        ChatUtil.message(sender, "Check Chunk Unload: %s", plugin.getCslConfig().isCheckChunkUnload());
        ChatUtil.message(sender, "Active Inspection: %s", plugin.getCslConfig().isActiveInspections());
        ChatUtil.message(sender, "Watch Creature Spawns: %s", plugin.getCslConfig().isWatchCreatureSpawns());
        ChatUtil.message(sender, "Check Surrounding Chunks: %s", plugin.getCslConfig().getCheckSurroundingChunks());
        ChatUtil.message(sender, "Inspection Frequency: %d", plugin.getCslConfig().getInspectionFrequency());
        ChatUtil.message(sender, "Notify Players: %s", plugin.getCslConfig().isNotifyPlayers());
        ChatUtil.message(sender, "Preserve Named Entities: %s", plugin.getCslConfig().isPreserveNamedEntities());
        ChatUtil.message(sender, "Ignore Metadata: %s", plugin.getCslConfig().getIgnoreMetadata().toString());
        ChatUtil.message(sender, "Worlds Mode: %s", plugin.getCslConfig().getWorldsMode().name());
        ChatUtil.message(sender, "Worlds: %s", plugin.getCslConfig().getWorldNames());
        ChatUtil.message(sender, "&2&l-- Messages --");
        ChatUtil.message(sender, "Reloaded Config: %s", plugin.getCslConfig().getReloadedConfig());
        ChatUtil.message(sender, "Removed Entities: %s", plugin.getCslConfig().getRemovedEntities());
    }

    @Subcommand(Command.Info.COMMAND)
    @CommandAlias(Command.Info.ALIAS)
    @CommandPermission(Command.Info.PERMISSION)
    @Description(Command.Info.DESCRIPTION)
    public void onInfo(final CommandSender sender) {
        ChatUtil.message(sender, "&2&l-- ChunkSpawnerLimiter v%s --", plugin.getDescription().getVersion());
        ChatUtil.message(sender, "&2&l-- Paper? %b, Armor Tick? %b", Util.isPaperServer(), Util.isArmorStandTickDisabled());
        ChatUtil.message(sender, "&2&l-- Reasons to cull on: --");
        ChatUtil.message(sender, plugin.getCslConfig().getFormattedSpawnReasons());
        ChatUtil.message(sender, "&2&l-- Entity Limits: --");
        ChatUtil.message(sender, plugin.getCslConfig().getFormattedEntityLimits());
    }

    @Subcommand(Command.Search.COMMAND)
    @CommandAlias(Command.Search.ALIAS)
    @CommandPermission(Command.Search.PERMISSION)
    @Description(Command.Search.DESCRIPTION)
    public void onSearch(final CommandSender sender, @Optional final EntityType entity) {
        if (entity != null) {
            ChatUtil.message(sender, entity.name());
            return;
        }

        ChatUtil.message(sender, StringUtils.join(EntityType.values(), ", "));
    }

    @Private
    @Subcommand("check")
    @CommandAlias("cslcheck")
    @CommandPermission("csl.check")
    @Description("Debug command for checking the amount & limit of an entity in the current chunk")
    public void onCheck(final @NotNull Player player, final @NotNull EntityType entityType) {
        final int limit = plugin.getCslConfig().getEntityLimit(entityType.name());
        int size = 0;
        final WeakReference<Chunk> weakChunk = new WeakReference<>(player.getLocation().getWorld().getChunkAt(player.getLocation()));
        if (weakChunk.get() == null) {
            ChatUtil.message(player, "Chunk was unloaded.. somehow");
            return;
        }

        final Chunk chunk = weakChunk.get();
        if (chunk == null) {
            ChatUtil.message(player, "Chunk was unloaded.. somehow");
            return;
        }

        for (final Entity entity : chunk.getEntities()) {
            if (entity.getType() == entityType) {
                size++;
            }
        }

        ChatUtil.message(player, "EntityType %s Limit: %d, Size: %d", entityType.name(), limit, size);
    }


    /**
     * This function handles the help command for the ChunkSpawnerLimiter plugin.
     *
     * @param help An instance of the CommandHelp class, which provides methods for displaying help information.
     */
    @HelpCommand
    public void onHelp(final CommandHelp help) {
        help.showHelp();
    }
}
