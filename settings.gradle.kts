rootProject.name = "ChunkSpawnerLimiter"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.0.2")
            library("acf", "co.aikar:acf-paper:0.5.1-SNAPSHOT")
            library("annotations", "org.jetbrains:annotations:24.1.0")
            library("nbt-api", "de.tr7zw:item-nbt-api:2.13.1")

            plugin("plugin-yml","net.minecrell.plugin-yml.bukkit").version("0.6.0")
            plugin("shadow","com.github.johnrengelman.shadow").version("8.1.1")
        }
    }
}