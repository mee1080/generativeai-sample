pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
//        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}
rootProject.name = "generativeai"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
