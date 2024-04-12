import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.*

plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.konfig)
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serializationJson)
                implementation(libs.generativeAi)
                runtimeOnly(libs.slf4j.nop)
            }
        }
    }
}

buildkonfig {
    packageName = "io.github.mee1080.generativeai"
    val localPropsFile = rootProject.file("local.properties")
    val localProperties = Properties()
    localProperties.load(localPropsFile.inputStream())
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "GEMINI_API_KEY", localProperties["gemini_api_key"]?.toString() ?: "")
        buildConfigField(FieldSpec.Type.STRING, "OPEN_ROUTER_KEY", localProperties["open_router_key"]?.toString() ?: "")
    }
}