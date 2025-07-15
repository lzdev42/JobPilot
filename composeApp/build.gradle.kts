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
            packageName = "xyz.emuci.jobpilot"
            packageVersion = "1.0.0"

            // Gemini 修改开始：根据当前操作系统动态设置打包格式
            // 这样，在macOS上构建时不会因包含AppImage而失败。
            // Windows的打包将由CI工作流特殊处理，以生成.zip文件。
            val osName = System.getProperty("os.name")
            if (osName.startsWith("Mac")) {
                targetFormats(TargetFormat.Dmg)
            } else if (osName.startsWith("Linux")) {
                targetFormats(TargetFormat.AppImage)
            } else if (osName.startsWith("Windows")){
                targetFormats(TargetFormat.Msi)
            }
            // Gemini 修改结束
        }

        // Gemini 修改开始：根据文章内容添加ProGuard发布配置
        buildTypes {
            release {
                proguard {
                    // 应用ProGuard规则文件
                    configurationFiles.from(file("proguard-rules.pro"))
                    // 指定兼容Java 21和Kotlin 2.x的ProGuard版本
                    version.set("7.7.0")
                }
            }
        }
        // Gemini 修改结束
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("xyz.emuci.jobpilot.MainKt")
}