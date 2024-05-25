rootProject.name = "ChunkSpawnerLimiter"

include(":chunkspawnerlimiter-core")
include(":chunkspawnerlimiter-spigot")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("configurate-core", "org.spongepowered:configurate-core:4.2.0-SNAPSHOT")
            library("configurate-yaml", "org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT")
            library("spigot-api", "org.spigotmc:spigot-api:1.14.4-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.0.2")
            library("acf", "co.aikar:acf-paper:0.5.1-SNAPSHOT")
            library("annotations", "org.jetbrains:annotations:24.0.1")
            library("nbt-api", "de.tr7zw:item-nbt-api:2.12.4")

            plugin("plugin-yml","net.minecrell.plugin-yml.bukkit").version("0.6.0")
            plugin("shadow","com.github.johnrengelman.shadow").version("8.1.1")
        }
    }
}