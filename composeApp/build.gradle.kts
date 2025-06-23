import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

val ktor_version: String by project

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.1.21"
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("io.ktor:ktor-client-core:${ktor_version}")
            implementation("io.ktor:ktor-client-cio:${ktor_version}")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            implementation("com.microsoft.playwright:playwright:1.52.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "xyz.emuci.jobpilot.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "xyz.emuci.jobpilot"
            packageVersion = "1.0.0"
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("xyz.emuci.jobpilot.MainKt")
}