package com.cyprias.chunkspawnerlimiter.listeners;

import com.cyprias.chunkspawnerlimiter.messages.Debug;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import com.cyprias.chunkspawnerlimiter.utils.ChunkSnapshotCache;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author sarhatabaot
 */
public class PlaceBlockListener implements Listener {
    private final ChunkSpawnerLimiter plugin;
    private final ChunkSnapshotCache chunkSnapshotCache;

    public PlaceBlockListener(final ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
        this.chunkSnapshotCache = new ChunkSnapshotCache(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled() || !plugin.getBlocksConfig().isEnabled()) {
            return;
        }

        if (plugin.getCslConfig().isWorldNotAllowed(event.getBlock().getChunk().getWorld().getName())) {
            return;
        }

        final Material placedType = event.getBlock().getType();
        if (!plugin.getBlocksConfig().hasLimit(placedType)) {
            return;
        }


        final Integer limit = plugin.getBlocksConfig().getLimit(placedType);
        final int minY = getMinY(event.getBlock().getWorld());
        final int maxY = getMaxY(event.getBlock().getWorld());
        event.setCancelled(true);

        final Chunk chunk = event.getBlock().getChunk();

        // Hybrid check
        int currentCount = chunkSnapshotCache.getMaterialCount(chunk, placedType, minY, maxY, limit);
        ChatUtil.debug(Debug.SCAN_LIMIT, placedType, currentCount, limit > plugin.getBlocksConfig().getMinLimitForCache());
        if (currentCount <= limit) {
            event.setCancelled(false);
            chunkSnapshotCache.updateMaterialCount(chunk, placedType, +1, limit); // Update cache if needed
            return;
        }

        // Blocked due to limit; cache remains valid
        if (plugin.getBlocksConfig().isNotifyMessage()) {
            ChatUtil.message(
                    event.getPlayer(), plugin.getCslConfig().getMaxAmountBlocks()
                            .replace("{material}", placedType.name())
                            .replace("{amount}", String.valueOf(limit))
            );
        }

        if (plugin.getBlocksConfig().isNotifyTitle()) {
            ChatUtil.title(
                    event.getPlayer(),
                    plugin.getCslConfig().getMaxAmountBlocksTitle(),
                    plugin.getCslConfig().getMaxAmountBlocksSubtitle(),
                    placedType.name(),
                    limit
            );
        }

        ChatUtil.debug(Debug.BLOCK_PLACE_CHECK, placedType, currentCount, limit);
    }

    @EventHandler
    public void onBreak(@NotNull BlockBreakEvent event) {
        Material brokenType = event.getBlock().getType();
        if (plugin.getBlocksConfig().hasLimit(brokenType)) {
            int limit = plugin.getBlocksConfig().getLimit(brokenType);
            chunkSnapshotCache.updateMaterialCount(event.getBlock().getChunk(), brokenType, -1, limit);
        }
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        chunkSnapshotCache.invalidate(event.getChunk());
    }

    private int getMinY(final @NotNull World world) {
        if (plugin.getBlocksConfig().hasWorld(world.getName())) {
            return plugin.getBlocksConfig().getMinY(world.getName());
        }
        switch (world.getEnvironment()) {
            case NORMAL:
                return plugin.getBlocksConfig().getMinY();
            case NETHER:
            case THE_END:
            default:
                return 0;
        }
    }

    private int getMaxY(final @NotNull World world) {
        if (plugin.getBlocksConfig().hasWorld(world.getName())) {
            return plugin.getBlocksConfig().getMaxY(world.getName());
        }
        return plugin.getBlocksConfig().getMaxY();
    }

}
