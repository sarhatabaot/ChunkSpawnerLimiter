plugins {
    java
}

group = "com.cyprias.chunkspawnerlimiter"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)
    implementation(libs.annotations)
}

