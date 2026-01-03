package com.github.sarhatabaot.chunkspawnerlimiter.notification;

import com.github.sarhatabaot.chunkspawnerlimiter.PluginConfig;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player notifications for entity and block limit events.
 * Includes throttling to prevent spam.
 */
public class NotificationService {
    private final PluginConfig config;
    
    // Throttling: track last notification time per player
    private final Map<UUID, Long> lastNotificationTime = new ConcurrentHashMap<>();
    
    public NotificationService(PluginConfig config) {
        this.config = config;
    }
    
    /**
     * Notify players in a chunk that entities were blocked from spawning
     */
    public void notifyEntitiesBlocked(Chunk chunk, EntityType type, int count) {
        if (!config.shouldNotifyPlayersInChunk()) return;
        
        String message = config.getEntitiesBlockedMessage()
            .replace("{count}", String.valueOf(count))
            .replace("{type}", type.name());
        
        notifyPlayersInChunk(chunk, message);
    }
    
    /**
     * Notify players in a chunk that entities were removed
     */
    public void notifyEntitiesRemoved(Chunk chunk, EntityType type, int count) {
        if (!config.shouldNotifyPlayersInChunk()) return;
        
        String message = config.getEntitiesRemovedMessage()
            .replace("{count}", String.valueOf(count))
            .replace("{type}", type.name());
        
        notifyPlayersInChunk(chunk, message);
    }
    
    /**
     * Notify a specific player that they've reached a block limit
     */
    public void notifyBlockLimitReached(Player player, Material material, int limit) {
        if (!shouldNotifyPlayer(player)) return;
        
        if (config.shouldUseTitleNotifications()) {
            String title = config.getMaxBlocksTitle()
                .replace("{material}", material.name())
                .replace("{amount}", String.valueOf(limit));
            String subtitle = config.getMaxBlocksSubtitle()
                .replace("{material}", material.name())
                .replace("{amount}", String.valueOf(limit));
            
            player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle)
            );
        }
        
        if (config.shouldUseMessageNotifications()) {
            String message = config.getMaxBlocksMessage()
                .replace("{material}", material.name())
                .replace("{amount}", String.valueOf(limit));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        
        updateLastNotification(player);
    }
    
    /**
     * Send a notification message to all players in a chunk
     */
    private void notifyPlayersInChunk(Chunk chunk, String message) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player player) {
                if (shouldNotifyPlayer(player)) {
                    sendNotification(player, message);
                    updateLastNotification(player);
                }
            }
        }
    }
    
    /**
     * Send a notification to a player using configured method(s)
     */
    private void sendNotification(Player player, String message) {
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        
        if (config.shouldUseTitleNotifications()) {
            player.sendTitle("", colored);
        }
        
        if (config.shouldUseMessageNotifications()) {
            player.sendMessage(colored);
        }
    }
    
    /**
     * Check if enough time has passed since last notification to this player
     */
    private boolean shouldNotifyPlayer(Player player) {
        Long lastTime = lastNotificationTime.get(player.getUniqueId());
        if (lastTime == null) return true;
        
        long cooldownMs = config.getNotificationCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastTime > cooldownMs;
    }
    
    /**
     * Update the last notification time for a player
     */
    private void updateLastNotification(Player player) {
        lastNotificationTime.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Clean up notification tracking for a player (e.g., on logout)
     */
    public void cleanup(Player player) {
        lastNotificationTime.remove(player.getUniqueId());
    }
}
