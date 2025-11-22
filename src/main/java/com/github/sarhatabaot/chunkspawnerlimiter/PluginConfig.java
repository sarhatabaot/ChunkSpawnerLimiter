package com.github.sarhatabaot.chunkspawnerlimiter;

import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;


public class PluginConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    private Map<String, Integer> entityLimits;
    private Map<String, Integer> blockLimits;
    private Set<String> spawnReasons;
    private Map<String, List<String>> entityGroups;


    public PluginConfig(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.entityLimits = null;
        this.blockLimits = null;
        this.spawnReasons = null;
        this.entityGroups = null;
    }

    // Main settings
    public boolean isEnabled() {
        return config.getBoolean("enabled", false);
    }

    public boolean isDebugMessages() {
        return config.getBoolean("debug-messages", false);
    }

    public boolean isMetrics() {
        return config.getBoolean("metrics", true);
    }


    // In theory, this is covered by EntitySpawnEvent, so why do we need this? todo
    public boolean isCreatureSpawnWatch() {
        return config.getBoolean("events.spawn.creature", true);
    }

    public boolean isVehicleSpawnWatch() {
        return config.getBoolean("events.spawn.vehicle", true);
    }

    public boolean isEntitySpawnWatch() {
        return config.getBoolean("events.spawn.entity", true);
    }

    public boolean isActiveInspections() {
        return config.getBoolean("events.inspections.enabled", true);
    }

    public int getInspectionFrequency() {
        return config.getInt("events.inspections.frequency", 300);
    }

    public int getSurroundingChunksRadius() {
        return config.getInt("events.chunk.surrounding-chunks-radius", 1);
    }

    // Entity settings
    public Map<String, Integer> getEntityLimits() {
        if (this.entityLimits == null) {
            this.entityLimits = new HashMap<>();

            var limitsSection = config.getConfigurationSection("entities.limits");
            if (limitsSection == null) return Collections.emptyMap();

            this.entityLimits = limitsSection.getKeys(false).stream()
                    .collect(Collectors.toMap(
                            key -> key,
                            limitsSection::getInt
                    ));
        }

        return entityLimits;
    }

    /**
     * Checks for both entity limits & entity group
     * @param entity The entity being checked
     * @return true if it has a limit or matches a group
     */
    public boolean hasEntityLimit(final Entity entity) {
        final String entityGroup = getEntityGroup(entity);
        return hasEntityLimit(entity.getType().name()) || getEntityLimits().containsKey(entityGroup);
    }

    //todo, lots of similar code here regarding entity limits and groups, we need to really check what's needed.
    public Integer getEntityGroupLimit(final String group) {
        if (!entityGroups.containsKey(group) || !entityLimits.containsKey(group)) {
            return null;
        }

        return entityLimits.get(group);
    }

    public Integer getEntityLimit(final Entity entity) {
        final String entityType = entity.getType().name();
        if (getEntityLimits().containsKey(entityType)) {
            return getEntityLimits().get(entityType);
        }

        final String entityGroup = getEntityGroup(entity);
        return getEntityLimits().get(entityGroup);
    }
    public Integer getEntityLimit(final EntityType entityType) {
        final String entityTypeName = entityType.name();

        // First check if the entity type has a direct limit
        if (getEntityLimits().containsKey(entityTypeName)) {
            return getEntityLimits().get(entityTypeName);
        }

        // Then check if it belongs to a group that has a limit
        final String entityGroup = getEntityGroup(entityType);
        if (entityGroup != null && getEntityLimits().containsKey(entityGroup)) {
            return getEntityLimits().get(entityGroup);
        }

        return null;
    }

    public String getEntityGroup(final EntityType entityType) {
        Map<String, List<String>> entityGroups = getEntityGroups();
        for (Map.Entry<String, List<String>> entry : entityGroups.entrySet()) {
            if (entry.getValue().contains(entityType.name())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean hasEntityLimit(final EntityType entityType) {
        return getEntityLimit(entityType) != null;
    }

    public Map<String, List<String>> getEntityGroups() {
        if (this.entityGroups == null) {
            this.entityGroups = new HashMap<>();

            var groupsSection = config.getConfigurationSection("entities.entity-groups");
            if (groupsSection == null) return Collections.emptyMap();

            for (String groupName : groupsSection.getKeys(false)) {
                List<String> entities = groupsSection.getStringList(groupName);
                this.entityGroups.put(groupName.toUpperCase(), entities.stream()
                        .map(String::toUpperCase)
                        .collect(Collectors.toList()));
            }
        }
        return entityGroups;
    }

    public boolean hasEntityLimit(final String entityType) {
        return getEntityLimits().containsKey(entityType);
    }

    public boolean hasBlockLimit(final String material) {
        return getBlockLimits().containsKey(material);
    }

    public RemovalMode getRemovalMode() {
        var mode = config.getString("entities.removal.mode", "enforce");
        return RemovalMode.fromString(mode);
    }

    public boolean shouldDropArmorStandItems() {
        return config.getBoolean("entities.removal.armor-stand.drop", false);
    }

    public boolean shouldLogArmorStandWarnings() {
        return config.getBoolean("entities.removal.armor-stand.log-warnings", true);
    }

    public boolean shouldPreserveNamedEntities() {
        return config.getBoolean("entities.preservation.named-entities", true);
    }

    public boolean shouldPreserveRaidEntities() {
        return config.getBoolean("entities.preservation.raid-entities", true);
    }

    public List<String> getIgnoreMetadata() {
        return Objects.requireNonNullElse(
                config.getStringList("entities.ignore.metadata"),
                List.of("shopkeeper")
        );
    }

    public List<String> getIgnoreNbt() {
        return Objects.requireNonNullElse(
                config.getStringList("entities.ignore.nbt"),
                Collections.emptyList()
        );
    }

    public Set<String> getSpawnReasons() {
        if (spawnReasons == null) {
            var reasonsList = config.getStringList("spawn-reasons");
            if (reasonsList == null || reasonsList.isEmpty()) {
                return getDefaultSpawnReasons();
            }

            this.spawnReasons = new HashSet<>(reasonsList);
        }

        return spawnReasons;
    }

    private @NotNull @Unmodifiable Set<String> getDefaultSpawnReasons() {
        return Set.of(
                "BREEDING",
                "BUILD_IRONGOLEM",
                "BUILD_SNOWMAN",
                "BUILD_WITHER",
                "CHUNK_GEN",
                "DEFAULT",
                "DISPENSE_EGG",
                "DROWNED",
                "EGG",
                "JOCKEY",
                "LIGHTNING",
                "MOUNT",
                "NATURAL",
                "NETHER_PORTAL",
                "OCELOT_BABY",
                "REINFORCEMENTS",
                "SILVERFISH_BLOCK",
                "SPAWNER",
                "SPAWNER_EGG",
                "TRAP",
                "VILLAGE_DEFENSE",
                "VILLAGE_INVASION"
        );
    }

    // Block limits
    public Map<String, Integer> getBlockLimits() {
        if (blockLimits == null) {
            var blocksSection = config.getConfigurationSection("blocks");
            if (blocksSection == null) return Collections.emptyMap();

            this.blockLimits = blocksSection.getKeys(false).stream()
                    .collect(Collectors.toMap(
                            key -> key,
                            blocksSection::getInt
                    ));
        }

        return blockLimits;
    }

    // World settings
    public String getWorldsMode() {
        return config.getString("worlds.mode", "excluded");
    }

    public List<String> getWorldsList() {
        return Objects.requireNonNullElse(
                config.getStringList("worlds.list"),
                List.of()
        );
    }
    private static final String DEFAULT_STRING = "default";


    // Notification settings
    public boolean shouldNotifyPlayersInChunk() {
        return config.getBoolean("notifications.players-in-chunk", false);
    }

    public boolean shouldUseTitleNotifications() {
        return config.getBoolean("notifications.method.title", true);
    }

    public boolean shouldUseMessageNotifications() {
        return config.getBoolean("notifications.method.message", false);
    }

    public String getEntitiesRemovedMessage() {
        return config.getString("notifications.messages.entities-removed", "&7Removed %s %s in your chunk.");
    }

    public String getReloadCompleteMessage() {
        return config.getString("notifications.messages.reload-complete", "&cReloaded csl config.");
    }

    public String getMaxBlocksMessage() {
        return config.getString("notifications.messages.max-blocks", "&6Cannot place more &4{material}&6. Max amount per chunk &2{amount}.");
    }

    public @NotNull String getEntityGroup(
            @NotNull Entity entity
    ) {
        // 1️⃣ Try to find a match in config
        final String fromConfig = getGroupFromConfig(entity.getType(), config);
        if (fromConfig != null) {
            return fromConfig;
        }

        return getGroupFromInstance(entity);
    }

    private @Nullable String getGroupFromConfig(
            EntityType type,
            @NotNull FileConfiguration config
    ) {
        ConfigurationSection section = config.getConfigurationSection("entities.entity-groups");
        if (section == null) {
            return null;
        }

        for (String group : section.getKeys(false)) {
            List<String> members = section.getStringList("group");
            if (members.contains(type.name())) {
                return group.toUpperCase();
            }
        }
        return null;
    }


    private @NotNull String getGroupFromInstance(Entity entity) {
        if (entity instanceof Animals) {
            return "ANIMALS";
        }
        if (entity instanceof Monster) {
            return "MONSTER";
        }
        if (entity instanceof NPC) {
            return "NPC";
        }
        if (entity instanceof WaterMob) {
            return "WATER_MOB";
        }
        if (entity instanceof Ambient) {
            return "AMBIENT";
        }
        if (entity instanceof Golem) {
            return "GOLEM";
        }
        if (entity instanceof Vehicle) {
            return "VEHICLE";
        }
        return entity.getType().name();
    }

    public boolean isWorldDisabled(final String worldName) {
        if (getWorldsMode().equalsIgnoreCase("exclude")) {
            return getWorldsList().contains(worldName);
        }
        return !getWorldsList().contains(worldName);
    }

    public boolean isKillPlayers() {
        return config.getBoolean("entities.removal.kill-players", false);
    }

}