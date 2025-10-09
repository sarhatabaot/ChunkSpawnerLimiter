plugins {
    java
    alias(libs.plugins.run.paper)
}

group = "com.github.sarhatabaot"
version = "5.0.0-ALPHA"

dependencies {
    compileOnly(libs.spigot.api)

    implementation(libs.bstats)
    implementation(libs.annotations)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}