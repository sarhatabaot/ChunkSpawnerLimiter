rootProject.name = "ChunkSpawnerLimiter"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public")
        maven("https://repo.aikar.co/content/groups/aikar/")
    }

    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.14.4-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.1.0")
            library("acf", "co.aikar:acf-paper:0.5.1-SNAPSHOT")
            library("annotations", "org.jetbrains:annotations:26.0.2")

            plugin("plugin-yml","net.minecrell.plugin-yml.bukkit").version("0.6.0")
            plugin("shadow","com.gradleup.shadow").version("8.3.1")
        }
    }
}