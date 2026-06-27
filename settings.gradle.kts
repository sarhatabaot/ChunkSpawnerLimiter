rootProject.name = "ChunkSpawnerLimiter"

dependencyResolutionManagement {
    repositories {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.loohpjames.com/repository/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    versionCatalogs {
        create("libs") {
            library("spigot-api", "org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
            library("paper-api", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
            library("bstats", "org.bstats:bstats-bukkit:3.2.1")
            library("annotations", "org.jetbrains:annotations:26.1.0")
            library("commands", "com.github.despical:command-framework:1.5.4")
            library("nbt-api", "de.tr7zw:item-nbt-api-plugin:2.15.7")
            library("jcip", "com.google.code.findbugs:jsr305:3.0.2")

            library("junit-api", "org.junit.jupiter:junit-jupiter-api:5.14.4")
            library("junit-engine", "org.junit.jupiter:junit-jupiter-engine:5.14.4")
            library("mockito-core", "org.mockito:mockito-core:5.23.0")
            library("mockito-junit-jupiter", "org.mockito:mockito-junit-jupiter:5.23.0")
            library("assertj-core", "org.assertj:assertj-core:3.27.7")

            library("adventure-api", "net.kyori:adventure-api:4.26.1")
            library("mockbukkit", "org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0")
            library("mockbukkit-legacy", "com.github.MockBukkit:MockBukkit:2ab2b498cd")

            plugin("run-paper", "xyz.jpenilla.run-paper").version("2.3.1")
            plugin("shadow", "com.gradleup.shadow").version("9.4.3")
            plugin("plugin-yml", "de.eldoria.plugin-yml.bukkit").version("0.8.0")
        }
    }

}
