package xyz.emuci.jobpilot

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.charset.StandardCharsets

fun main() {
    // 设置 JVM 默认编码为 UTF-8，解决 Windows 下中文输出乱码问题
    // macOS/Linux 默认就是 UTF-8，但 Windows 可能是 GBK
    System.setProperty("file.encoding", "UTF-8")
    java.lang.System.setProperty("file.encoding", "UTF-8")
    
    // 重新创建标准输出流，确保使用 UTF-8 编码
    // 这可以解决 Windows 控制台中文乱码问题
    try {
        val originalOut = System.out
        val originalErr = System.err
        System.setOut(java.io.PrintStream(originalOut, true, StandardCharsets.UTF_8.name()))
        System.setErr(java.io.PrintStream(originalErr, true, StandardCharsets.UTF_8.name()))
    } catch (e: Exception) {
        // 如果设置失败，至少系统属性已设置，大部分情况下已足够
        // 某些 JVM 版本可能不支持在运行时修改输出流编码
    }
    
    application {
	Window(
		onCloseRequest = ::exitApplication,
		title = "JobPilot",
	) {
		AppInitializer.initialize()
		App()
	}
    }
}