import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
}

group = "com.github.sarhatabaot"
version = "5.0.0-ALPHA"
description = "Limit blocks & entities in chunks."

dependencies {
    compileOnly(libs.spigot.api)

    implementation(libs.bstats)
    implementation(libs.annotations)

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
    //todo api-version="1.8" since we are supporting 1.8, this may block the loading? idk need to test
}

tasks {
    runServer {
        //use this to manually test various version load, probably should use docker with ci/cd for the automated version
        //todo amazing 1.13-1.16 breaks with jvm 21, the rest works, lmao, mention this on the website
        minecraftVersion("1.21.10")
        jvmArgs("-Dcom.mojang.eula.agree=true")
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