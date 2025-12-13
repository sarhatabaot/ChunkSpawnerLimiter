package com.github.sarhatabaot.chunkspawnerlimiter;

import com.github.sarhatabaot.chunkspawnerlimiter.removal.modes.RemovalMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;


public class PluginConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    private Map<EntityType, Integer> directEntityLimits;
    private Map<EntityType, Integer> resolvedEntityLimits;
    private Map<EntityType, String> entityToGroup;

    private Map<Material, Integer> directBlockLimits;
    private Map<Material, Integer> resolvedBlockLimits;
    private Map<Material, String> blockToGroup;

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

        loadEntityGroups();
        loadEntityLimits();

        loadBlockGroups();
        loadBlockLimits();

        loadSpawnReasons();
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

    private void loadBlockGroups() {
        blockToGroup = new EnumMap<>(Material.class);

        ConfigurationSection section = config.getConfigurationSection("blocks.entity-groups");
        if (section == null) return;

        for (String group: section.getKeys(false)){
            for (String member : section.getStringList(group)) {
                Material type = Material.valueOf(member.toUpperCase());
                blockToGroup.put(type, group.toUpperCase());
            }
        }
    }

    private void loadBlockLimits() {
        directBlockLimits = new EnumMap<>(Material.class);
        resolvedBlockLimits = new EnumMap<>(Material.class);

        Map<String, Integer> rawLimits = getEntityLimits();

        // 1️⃣ Direct type limits
        for (Material type : Material.values()) {
            Integer limit = rawLimits.get(type.name());
            if (limit != null) {
                directBlockLimits.put(type, limit);
                resolvedBlockLimits.put(type, limit);
            }
        }

        // 2️⃣ Group limits (only if no direct limit)
        for (Map.Entry<Material, String> entry : blockToGroup.entrySet()) {
            Material type = entry.getKey();
            String group = entry.getValue();

            if (resolvedBlockLimits.containsKey(type)) {
                continue; // type limit wins
            }

            Integer groupLimit = rawLimits.get(group);
            if (groupLimit != null) {
                resolvedBlockLimits.put(type, groupLimit);
            }
        }
    }
    public Integer getResolvedBlockLimit(Material type) {
        return resolvedBlockLimits.get(type);
    }

    public boolean hasResolvedBlockLimit(Material type) {
        return resolvedBlockLimits.containsKey(type);
    }

    private void loadEntityGroups() {
        entityToGroup = new EnumMap<>(EntityType.class);

        ConfigurationSection section = config.getConfigurationSection("entities.entity-groups");
        if (section == null) return;

        for (String group : section.getKeys(false)) {
            for (String member : section.getStringList(group)) {
                EntityType type = EntityType.valueOf(member.toUpperCase());
                entityToGroup.put(type, group.toUpperCase());
            }
        }
    }

    private void loadEntityLimits() {
        directEntityLimits = new EnumMap<>(EntityType.class);
        resolvedEntityLimits = new EnumMap<>(EntityType.class);

        Map<String, Integer> rawLimits = getEntityLimits();

        // 1️⃣ Direct type limits
        for (EntityType type : EntityType.values()) {
            Integer limit = rawLimits.get(type.name());
            if (limit != null) {
                directEntityLimits.put(type, limit);
                resolvedEntityLimits.put(type, limit);
            }
        }

        // 2️⃣ Group limits (only if no direct limit)
        for (Map.Entry<EntityType, String> entry : entityToGroup.entrySet()) {
            EntityType type = entry.getKey();
            String group = entry.getValue();

            if (resolvedEntityLimits.containsKey(type)) {
                continue; // type limit wins
            }

            Integer groupLimit = rawLimits.get(group);
            if (groupLimit != null) {
                resolvedEntityLimits.put(type, groupLimit);
            }
        }
    }

    public Integer getResolvedEntityLimit(EntityType type) {
        return resolvedEntityLimits.get(type);
    }

    public boolean hasResolvedEntityLimit(EntityType type) {
        return resolvedEntityLimits.containsKey(type);
    }

    private void loadSpawnReasons() {
        List<String> reasonsList = config.getStringList("spawn-reasons");

        if (reasonsList == null || reasonsList.isEmpty()) {
            spawnReasons = getDefaultSpawnReasons();
        } else {
            spawnReasons = new HashSet<>(reasonsList);
        }
    }


    // Entity settings
    @Deprecated
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
    @Deprecated
    public boolean hasEntityLimit(final Entity entity) {
        final String entityGroup = getEntityGroup(entity);
        return hasEntityLimit(entity.getType().name()) || getEntityLimits().containsKey(entityGroup);
    }

    //todo, lots of similar code here regarding entity limits and groups, we need to really check what's needed.
    @Deprecated
    public Integer getEntityGroupLimit(final String group) {
        if (!entityGroups.containsKey(group) || !entityLimits.containsKey(group)) {
            return null;
        }

        return entityLimits.get(group);
    }


    @Deprecated
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

    @Deprecated
    public String getEntityGroup(final EntityType entityType) {
        Map<String, List<String>> entityGroups = getEntityGroups();
        for (Map.Entry<String, List<String>> entry : entityGroups.entrySet()) {
            if (entry.getValue().contains(entityType.name())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Deprecated
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

    // group stuff
    @Deprecated
    public String getEntityGroup(
            @NotNull Entity entity
    ) {
        return getGroupFromConfig(entity.getType(), config);
    }

    @Deprecated
    private @Nullable String getGroupFromConfig(
            EntityType type,
            @NotNull FileConfiguration config
    ) {
        ConfigurationSection section = config.getConfigurationSection("entities.entity-groups");
        if (section == null) {
            return null;
        }

        for (String group : section.getKeys(false)) {
            List<String> members = section.getStringList(group);
            if (members.contains(type.name())) {
                return group.toUpperCase();
            }
        }
        return null;
    }

    @Deprecated
    public boolean hasEntityLimit(final String entityType) {
        return getEntityLimits().containsKey(entityType);
    }

    @Deprecated
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
        return spawnReasons;
    }

    private @NotNull Set<String> getDefaultSpawnReasons() {
        return Arrays.stream(CreatureSpawnEvent.SpawnReason.values())
                .map(CreatureSpawnEvent.SpawnReason::name).collect(Collectors.toUnmodifiableSet());
    }

    // Block limits
    @Deprecated
    public Map<String, Integer> getBlockLimits() {
        if (blockLimits == null) {
            var blocksSection = config.getConfigurationSection("blocks.limits");
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