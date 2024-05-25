package com.cyprias.chunkspawnerlimiter.listeners;

import com.cyprias.chunkspawnerlimiter.messages.Debug;
import com.cyprias.chunkspawnerlimiter.utils.ChatUtil;
import com.cyprias.chunkspawnerlimiter.ChunkSpawnerLimiter;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author sarhatabaot
 */
public class PlaceBlockListener implements Listener {
    private final ChunkSpawnerLimiter plugin;

    public PlaceBlockListener(final ChunkSpawnerLimiter plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlace(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled() || !plugin.getBlocksConfig().isEnabled())
            return;

        if (plugin.getCslConfig().getExcludedWorlds().contains(event.getBlock().getChunk().getWorld().getName()))
            return;

        final Material placedType = event.getBlock().getType();
        if (plugin.getBlocksConfig().hasLimit(placedType)) {
            final Integer limit = plugin.getBlocksConfig().getLimit(placedType);
            final int minY = getMinY(event.getBlock().getWorld());
            final int maxY = getMaxY(event.getBlock().getWorld());
            final int amountInChunk = countBlocksInChunk(event.getBlock().getChunk().getChunkSnapshot(), placedType, minY, maxY);
            if (amountInChunk > limit) {
                event.setCancelled(true);

                if (plugin.getBlocksConfig().isNotifyMessage()) {
                    ChatUtil.message(event.getPlayer(), plugin.getCslConfig().getMaxAmountBlocks()
                            .replace("{material}", placedType.name())
                            .replace("{amount}", String.valueOf(limit)));
                }
                if (plugin.getBlocksConfig().isNotifyTitle()) {
                    ChatUtil.title(event.getPlayer(),
                            plugin.getCslConfig().getMaxAmountBlocksTitle(),
                            plugin.getCslConfig().getMaxAmountBlocksSubtitle(),
                            placedType.name(),
                            limit);
                }
            }
            ChatUtil.debug(Debug.BLOCK_PLACE_CHECK, placedType, amountInChunk, limit);
        }
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


    private int countBlocksInChunk(final ChunkSnapshot chunkSnapshot, final Material material, final int minY, final int maxY) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunkSnapshot.getBlockType(x, y, z) == material)
                        count++;
                }
            }
        }
        return count;
    }

}
