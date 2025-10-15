rootProject.name = "ChunkSpawnerLimiter"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.loohpjames.com/repository/")
    }

    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.1.0")
            library("annotations", "org.jetbrains:annotations:26.0.2-1")
            library("commands", "com.github.despical:command-framework:1.5.3")

            plugin("run-paper", "xyz.jpenilla.run-paper").version("2.3.1")
            plugin("shadow", "com.gradleup.shadow").version("9.2.2")
            plugin("plugin-yml", "de.eldoria.plugin-yml.bukkit").version("0.8.0")
        }
    }
}