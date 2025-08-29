package xyz.emuci.jobpilot

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    // 启动前：确保系统 Java 21 可用，如在 AppImage 内置 JRE 中则切换到系统 JVM
    JvmPreflight.ensureSystemJavaOrRelaunch()
    Window(
        onCloseRequest = ::exitApplication,
        title = "JobPilot",
    ) {
        AppInitializer.initialize()
        App()
    }
}