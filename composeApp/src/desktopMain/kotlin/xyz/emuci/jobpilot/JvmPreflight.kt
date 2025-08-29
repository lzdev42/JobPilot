package xyz.emuci.jobpilot

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

object JvmPreflight {
    private const val REQUIRED_MAJOR = 21

    fun ensureSystemJavaOrRelaunch() {
        val currentJavaHome = System.getProperty("java.home") ?: ""
        val inAppImage = System.getenv("APPIMAGE") != null || currentJavaHome.contains(".mount_") || currentJavaHome.contains("AppImage")

        val systemJava = detectSystemJava() ?: run {
            // 无系统 Java，直接提示并退出
            showBlockingMessage(
                title = "缺少 Java 运行时",
                message = "未检测到系统 Java ${REQUIRED_MAJOR}+。请先安装 Java ${REQUIRED_MAJOR}+ 后再运行应用。"
            )
            exitProcess(1)
        }

        if (systemJava.major < REQUIRED_MAJOR) {
            showBlockingMessage(
                title = "Java 版本过低",
                message = "检测到系统 Java 版本为 ${systemJava.versionRaw}，需要 Java ${REQUIRED_MAJOR}+。请升级后再运行应用。"
            )
            exitProcess(1)
        }

        if (inAppImage) {
            // 在 AppImage 内置 JRE 环境，切换到系统 Java
            val classpath = System.getProperty("java.class.path") ?: ""
            if (classpath.isBlank()) {
                showBlockingMessage(
                    title = "无法获取类路径",
                    message = "无法获取应用类路径，无法切换到系统 Java。请反馈此问题。"
                )
                return
            }

            val env = buildMap {
                val home = System.getProperty("user.home") ?: "."
                val browsers = File(home, ".cache/ms-playwright").apply { mkdirs() }.absolutePath
                val varTmp = File("/var/tmp/jobpilot-tmp").apply { if (!exists()) runCatching { mkdirs() } }
                val tmp = if (varTmp.exists() && varTmp.canWrite()) varTmp.absolutePath else File(home, ".cache/jobpilot-tmp").apply { mkdirs() }.absolutePath
                put("PLAYWRIGHT_BROWSERS_PATH", browsers)
                put("TMPDIR", tmp)
                put("DEBUG", "pw:install,pw:download,pw:driver")
            }

            val ok = relaunchWithSystemJava(systemJava.path, classpath, env)
            if (ok) exitProcess(0)
            // 失败则继续当前进程（可能仍失败），但至少给出提示
            showBlockingMessage(
                title = "切换到系统 Java 失败",
                message = "已检测到系统 Java ${systemJava.versionRaw}，但切换失败，将继续使用内置 JRE 运行，可能导致浏览器驱动创建失败。"
            )
        }
    }

    private data class JavaInfo(val path: String, val versionRaw: String, val major: Int)

    private fun detectSystemJava(): JavaInfo? {
        return try {
            val which = ProcessBuilder("/usr/bin/env", "which", "java").redirectErrorStream(true).start()
            if (!which.waitFor(2, TimeUnit.SECONDS) || which.exitValue() != 0) return null
            val javaPath = which.inputStream.bufferedReader().readText().trim().lineSequence().firstOrNull()?.trim().orEmpty()
            if (javaPath.isBlank()) return null

            val ver = ProcessBuilder(javaPath, "-version").redirectErrorStream(true).start()
            if (!ver.waitFor(3, TimeUnit.SECONDS)) return null
            val verOut = ver.inputStream.bufferedReader().readText()
            val major = parseJavaMajor(verOut)
            JavaInfo(javaPath, verOut.lines().firstOrNull() ?: verOut.trim(), major ?: -1)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJavaMajor(versionOutput: String): Int? {
        // 示例：openjdk version "21.0.3"  /  java version "21" / "1.8.0_XXX"
        val m = Regex("\"(\\d+)(?:\\.\\d+)?").find(versionOutput) ?: return null
        val s = m.groupValues[1]
        return try { s.toInt() } catch (_: Exception) { null }
    }

    private fun relaunchWithSystemJava(javaPath: String, classpath: String, env: Map<String, String>): Boolean {
        return try {
            val args = mutableListOf(javaPath, "-cp", classpath, "xyz.emuci.jobpilot.MainKt")
            val pb = ProcessBuilder(args)
            val e = pb.environment()
            env.forEach { (k, v) -> e[k] = v }
            pb.inheritIO()
            pb.start()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun showBlockingMessage(title: String, message: String) {
        runCatching {
            javax.swing.JOptionPane.showMessageDialog(null, message, title, javax.swing.JOptionPane.ERROR_MESSAGE)
        }.onFailure {
            // 回退到控制台输出
            System.err.println("$title: $message")
        }
    }
}


