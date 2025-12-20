import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
}

group = "com.github.sarhatabaot"
version = "5.0.0-RC3"
description = "Limit blocks & entities in chunks."

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.nbt.api)

    implementation(libs.bstats)
    implementation(libs.annotations)
    implementation(libs.jcip)

    implementation(libs.commands)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
bukkit {
    name = rootProject.name
    main = "com.github.sarhatabaot.chunkspawnerlimiter.ChunkSpawnerLimiter"
    version = project.version.toString()
    website = "https://github.com/sarhatabaot/ChunkSpawnerLimiter"
    authors = listOf("Cyprias", "sarhatabaot")
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    prefix = "CSL"
    apiVersion = "1.13"
    softDepend = listOf("NBTAPI") //todo, this way we keep it small
}

tasks {
    runServer {
        //use this to manually test various version load, probably should use docker with ci/cd for the automated version
        //todo amazing 1.13-1.16 breaks with jvm 21, the rest works, lmao, mention this on the website
        minecraftVersion("1.20.1")
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }


    // Define your versions
    val minecraftVersions = listOf("1.8.8", "1.9.4", "1.12.2", "1.16.5", "1.20.1", "1.21.10")

    // Create tasks for each version
    minecraftVersions.forEach { version ->
        // Convert version to valid task name (replace dots with underscores)
        val taskName = "runServer${version.replace(".", "_")}"

        register(taskName) {
            group = "minecraft"
            description = "Run Minecraft server version $version"
            dependsOn(runServer)
            doFirst {
                // Set the minecraft version on the base task
                (runServer.get() as Task).extensions.extraProperties.set("minecraftVersion", version)
                (runServer.get() as Task).extensions.extraProperties.set("jvmArgs", "-Dcom.mojang.eula.agree=true")
                // Or if runServer has a property/method for setting version:
                // runServer.get().setMinecraftVersion(version)
            }
        }
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        minimize()

        archiveFileName.set("chunkspawnerlimiter-${project.version}.jar")
        archiveClassifier.set("shadow")

        exclude("META-INF/**")

        relocate("me.despical.commandframework","com.github.sarhatabaot.chunkspawnerlimiter.libs")
        relocate("org.bstats", "com.github.sarhatabaot.chunkspawnerlimiter.libs")
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(libs.junit.api)
                runtimeOnly(libs.junit.engine)
            }

            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform()
                    }
                }
            }
        }
    }
}