package com.github.sarhatabaot.chunkspawnerlimiter.command;

import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.annotations.Command;
import me.despical.commandframework.annotations.Completer;
import me.despical.commandframework.annotations.Confirmation;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AdminCommand {
    private final ChunkSpawnerLimiter plugin;

    public AdminCommand(ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "csl",
            aliases = {"csl.help", "csl.version"},
            permission = "csl"
    )
    public void onHelp(@NotNull CommandArguments arguments) {
        int page = arguments.getArgumentAsInt(0);

        showHelpPage(arguments.getSender(), page);
    }

    private void showHelpPage(CommandSender sender, int page) {
        List<String> helpPages = List.of(
                // Page 1 - Basic Commands
                """
                &6&lChunkSpawnerLimiter v%s Help &7(Page 1/2)
                &e/csl chunk info &7- Show chunk spawner info
                &7View current chunk's spawner counts
                &e/csl help [page] &7- Show help menu
                """.formatted(plugin.getDescription().getVersion()),
                """
                &6&lChunkSpawnerLimiter Help &7(Page 2/2)
                &6Admin Commands:
                &e/csl version &7- Show plugin version
                &e/csl reload &7- Reload configuration
                &e/csl search entities &7- List entity types
                &e/csl search blocks &7- List block materials
                &e/csl rebuild &7- Rebuild all counters
                &c  ⚠️ Can cause lag - use with caution
               """
        );

        int maxPage = helpPages.size();
        if (page < 1 || page > maxPage) {
            page = 1;
        }

        String pageContent = helpPages.get(page - 1);
        for (String line : pageContent.split("\n")) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }

        sender.sendMessage(org.bukkit.ChatColor.GRAY +
                "Use /csl help " + (page % maxPage + 1) + " for next page");
    }

    /*
    TODO
    The reload command should reload the config settings, and update the caches with the correct limits, if they exist.
    i.e. config settings should change. Counters won't be rebuilt unless specified with onRebuild.
     */
    @Command(
            name = "csl.reload",
            permission = "csl.reload"
    )
    public void onReload(CommandArguments arguments) {
        this.plugin.onReload();

        arguments.getSender().sendMessage("Reloaded config.");
    }

    @Command(
            name="csl.search.entities"
    )
    public void onSearchEntities(@NotNull CommandArguments arguments) {
        arguments.getSender().sendMessage(Arrays.stream(EntityType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", ")));
    }

    @Command(
            name="csl.search.entities"
    )
    public List<String> onSearchEntitiesCompletion() {
        return Arrays.stream(EntityType.values())
                .map(Enum::name)
                .toList();
    }

    @Command(
            name="csl.search.blocks"
    )
    public void onSearchBlocks(CommandArguments arguments) {
        arguments.getSender().sendMessage(Arrays.stream(Material.values())
                        .filter(material -> material != Material.AIR)
                .map(Enum::name)
                .collect(Collectors.joining(", ")));
    }

    @Completer(
            name = "csl.search.blocks"
    )
    public List<String> onSearchBlocksCompletion() {
        return Arrays.stream(Material.values())
                .map(Enum::name)
                .toList();
    }


    /*
    TODO
    Show specific chunk info counters. Either in a clickable list format or something like that.
    Optionally users should be able to view this, so they know when to stop placing?
    Mention that the user can see all the entity amounts using /spark profiler
    /csl chunk - shows all options - can click on a chunk to show info
    /csl chunk info - show current chunk info
    /csl chunk info <optional> - shows a specific chunk
     */
    public void onChunkInfo(CommandArguments commandArguments) {

    }


    //I'm still debating if it's actually worth using this, shouldn't we just restart the server? todo
    @Command(
            name="csl.rebuild"
    )
    @Confirmation(
            message = """
                    ⚠️ WARNING: This will rebuild every counter on the server.
                    The process can be slow and may cause lag or instability.
                    It’s recommended to restart the server instead.
                    To proceed anyway, re-run this command within 10 seconds.""",
            expireAfter = 10,
            bypassPerm = "csl.rebuild.bypass",
            timeUnit = TimeUnit.SECONDS,
            overrideConsole = true
    )
    public void onRebuild() {

    }

    //https://docker-minecraft-server.readthedocs.io/en/latest/

}
