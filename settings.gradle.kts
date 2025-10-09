rootProject.name = "ChunkSpawnerLimiter"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.1.0")
            library("annotations", "org.jetbrains:annotations:26.0.2-1")

            plugin("run-paper", "xyz.jpenilla.run-paper").version("2.3.1")
        }
    }
}