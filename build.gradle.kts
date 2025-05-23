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
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutinesCore)
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.clientLogging)
                implementation(libs.ktor.serializationKotlinxJson)
                implementation(libs.generativeAi)
                implementation(libs.koog.agents)
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
        buildConfigField(FieldSpec.Type.STRING, "MCP_FIRST_JAR", localProperties["mcp_first_jar"]?.toString() ?: "")
    }
}