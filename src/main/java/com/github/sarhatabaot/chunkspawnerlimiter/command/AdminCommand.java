package com.github.sarhatabaot.chunkspawnerlimiter.command;

import com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter;
import me.despical.commandframework.annotations.Command;
import me.despical.commandframework.annotations.Completer;
import me.despical.commandframework.annotations.Confirmation;

import java.util.concurrent.TimeUnit;

public class AdminCommand {
    private final ChunkSpawnerLimiter plugin;

    public AdminCommand(ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "csl.help",
            permission = "csl.help"
    )
    public void onHelp() {

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
    public void onReload() {
        this.plugin.reloadConfig();
    }

    /*
    TODO
    Simple version command.
     */
    @Command(
            name = "csl.version",
            permission = "csl.version"
    )
    public void onVersion() {

    }

    /*
    TODO
    Show available blocks or entities, ideally, these should be validated on start too.
     */
    @Command(
            name="csl.search.entities"
    )
    public void onSearchEntities() {

    }

    @Command(
            name="csl.search.entities"
    )
    public void onSearchEntitiesCompletion() {

    }

    @Command(
            name="csl.search.blocks"
    )
    public void onSearchBlocks() {

    }

    @Completer(
            name = "csl.search.blocks"
    )
    public void onSearchBlocksCompletion() {

    }


    /*
    TODO
    Show specific chunk info counters. Either in a clickable list format or something like that.
    Optionally users should be able to view this, so they know when to stop placing?
    /csl chunk - shows all options - can click on a chunk to show info
    /csl chunk info - show current chunk info
    /csl chunk info <optional> - shows a specific chunks
     */
    public void onChunkInfo(final String chunk) {

    }

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
