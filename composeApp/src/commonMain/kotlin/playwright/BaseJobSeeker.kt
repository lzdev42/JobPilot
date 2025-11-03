package playwright

import com.microsoft.playwright.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import logview.LogViewModel
import java.io.File
import java.nio.file.Paths

abstract class BaseJobSeeker(
    protected val viewModel: LogViewModel,
    protected val config: PlaywrightConfig
) {
    protected lateinit var browser: Browser
    protected lateinit var context: BrowserContext
    protected lateinit var page: Page

    // 仅在首次检测到 Playwright 安装/下载日志时提示一次
    @Volatile
    private var downloadNoticeShown: Boolean = false

    protected var submittedCount: Int = 0
    protected abstract val loginUrl: String
    protected abstract val homeUrl: String

    private fun isPlaywrightInstallLog(line: String): Boolean {
        val l = line.lowercase()
        return l.contains("pw:install") || l.contains("pw:download") ||
            l.contains("installing") || l.contains("downloading") ||
            l.contains("extracting") || l.contains("verifying") ||
            l.contains("chromium") || l.contains("driver")
    }


    // 初始化浏览器环境
    protected suspend fun init() {
        // 由 Playwright 自动检测和下载所需浏览器（首次运行可能较慢）
        viewModel.addLog("正在检查并准备 Chromium... 如未安装将自动下载（首次可能较慢）")

        // 确保可写缓存与临时目录，避免在只读或 noexec 目录导致 driver/下载失败
        val userHome = System.getProperty("user.home") ?: "."
        val browsersPath = Paths.get(userHome, ".cache", "ms-playwright").toFile().apply { mkdirs() }.absolutePath
        val tmpPath = Paths.get(userHome, ".cache", "jobpilot-tmp").toFile().apply { mkdirs() }.absolutePath

        // 在 Linux 下：将 JVM 的临时目录指向可写可执行目录，并确保权限（Playwright 驱动解压依赖此路径）
        val osName = System.getProperty("os.name")?.lowercase() ?: ""
        if (osName.contains("linux")) {
            System.setProperty("java.io.tmpdir", tmpPath)
            runCatching {
                val tmpDir = File(tmpPath)
                val browsersDir = File(browsersPath)
                tmpDir.setReadable(true, true)
                tmpDir.setWritable(true, true)
                tmpDir.setExecutable(true, true)
                browsersDir.setReadable(true, true)
                browsersDir.setWritable(true, true)
                browsersDir.setExecutable(true, true)
            }
        }

        // 构建 Playwright 创建选项：
        // - 开启安装/下载相关日志（不依赖应用的调试开关）
        // - 指定可写目录，保证 driver 创建与浏览器下载可以进行
        // - Windows 下补充 TMP/TEMP，避免在只读或受限目录解压失败
        val envMap = mutableMapOf(
            "DEBUG" to "pw:install,pw:download",
            "PLAYWRIGHT_BROWSERS_PATH" to browsersPath,
            "XDG_CACHE_HOME" to Paths.get(userHome, ".cache").toString(),
            "PLAYWRIGHT_DOWNLOAD_HOST" to "https://playwright.azureedge.net",
            "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD" to "false"
        )
        if (osName.contains("windows")) {
            envMap["TEMP"] = tmpPath
            envMap["TMP"] = tmpPath
        } else {
            envMap["TMPDIR"] = tmpPath
        }
        val options = Playwright.CreateOptions().setEnv(envMap)

        viewModel.addLog("开始初始化 Playwright 主实例...")

        // 将 Playwright 的安装/下载输出以常规日志展示（不依赖应用调试开关）
        val originalErr = System.err
        val originalOut = System.out
        val errTee = java.io.PrintStream(object : java.io.OutputStream() {
            private val buffer = StringBuilder()
            override fun write(b: Int) {
                originalErr.write(b)
                val ch = b.toChar()
                buffer.append(ch)
                if (ch == '\n') {
                    val line = buffer.toString().trimEnd()
                    buffer.setLength(0)
                    if (isPlaywrightInstallLog(line)) {
                        if (!downloadNoticeShown) {
                            viewModel.addLog("[Playwright] 正在下载浏览器组件，过程较慢且无法显示百分比，请耐心等待…")
                            downloadNoticeShown = true
                        }
                        viewModel.addLog("[Playwright] $line")
                    }
                }
            }
        }, true, Charsets.UTF_8)
        val outTee = java.io.PrintStream(object : java.io.OutputStream() {
            private val buffer = StringBuilder()
            override fun write(b: Int) {
                originalOut.write(b)
                val ch = b.toChar()
                buffer.append(ch)
                if (ch == '\n') {
                    val line = buffer.toString().trimEnd()
                    buffer.setLength(0)
                    if (isPlaywrightInstallLog(line)) {
                        if (!downloadNoticeShown) {
                            viewModel.addLog("[Playwright] 正在下载浏览器组件，过程较慢且无法显示百分比，请耐心等待…")
                            downloadNoticeShown = true
                        }
                        viewModel.addLog("[Playwright] $line")
                    }
                }
            }
        }, true, Charsets.UTF_8)

        // 在IO线程中创建并启动，若首次运行将阻塞直至下载完成
        val playwright = withContext(Dispatchers.IO) {
            try {
                System.setErr(errTee)
                System.setOut(outTee)
                Playwright.create(options)
            } catch (e: Exception) {
                viewModel.addLog("[警告] 创建 Playwright 失败：${e.message}，尝试清理临时目录后重试一次…")
                runCatching { File(tmpPath).deleteRecursively(); File(tmpPath).mkdirs() }
                try {
                    Playwright.create(options)
                } catch (e2: Exception) {
                    viewModel.addLog("[警告] 二次创建 Playwright 仍失败：${e2.message}，尝试切换镜像后再试…")
                    // 切换下载镜像源后再尝试一次（常见于网络受限环境）
                    envMap["PLAYWRIGHT_DOWNLOAD_HOST"] = "https://npmmirror.com/mirrors/playwright"
                    val optionsMirror = Playwright.CreateOptions().setEnv(envMap)
                    Playwright.create(optionsMirror)
                }
            } finally {
                System.setErr(originalErr)
                System.setOut(originalOut)
            }
        }
        viewModel.addLog("Playwright 主实例初始化完成。准备启动 Chromium 浏览器...")

        // 设置浏览器启动参数，使其在后台运行
        val baseArgs = listOf(
            "--window-position=0,0",  // 窗口位置
            "--window-size=1920,1080", // 窗口大小
            "--auto-open-devtools-for-tabs=false", // 不自动打开开发者工具
            "--disable-popup-blocking", // 禁用弹窗阻止
            "--disable-notifications", // 禁用通知
            "--noerrdialogs", // 禁用错误对话框
            "--disable-session-crashed-bubble" // 禁用会话崩溃气泡
        )
        val linuxSandboxArgs = if (osName.contains("linux")) listOf("--no-sandbox", "--disable-setuid-sandbox") else emptyList()
        val args = baseArgs + linuxSandboxArgs
        
        browser = withContext(Dispatchers.IO) {
            try {
                System.setErr(errTee)
                System.setOut(outTee)
                playwright.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(config.headless)
                        .setArgs(args)
                )
            } catch (e: Exception) {
                // 若因浏览器未安装导致启动失败，记录并引导再次创建时触发安装
                viewModel.addLog("[警告] 启动 Chromium 失败：${e.message}，将尝试再次初始化后重启…")
                System.setErr(originalErr)
                System.setOut(originalOut)
                // 重新走一次创建流程（保留可能已切换的镜像设置）
                System.setErr(errTee)
                System.setOut(outTee)
                val pw2 = Playwright.create(Playwright.CreateOptions().setEnv(envMap))
                pw2.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(config.headless)
                        .setArgs(args)
                )
            } finally {
                System.setErr(originalErr)
                System.setOut(originalOut)
            }
        }
        viewModel.addLog("Chromium 已准备并启动（若需下载已自动完成）。准备创建新的浏览器上下文...")

        context = browser.newContext( // 创建浏览器上下文
            Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setUserAgent(config.userAgent)
        )
        viewModel.addLog("浏览器上下文已创建。准备打开新页面...")

        page = context.newPage() // 打开新页面
        viewModel.addLog("新页面已打开。基础环境初始化完成。")
    }


    // --- 以下是原有的页面操作和业务逻辑方法，注释保持原样或根据需要自行调整 ---

    // 基础页面操作
    protected suspend fun navigate(url: String) = page.navigate(url)
    protected suspend fun click(selector: String) = page.click(selector)
    protected suspend fun fill(selector: String, text: String) = page.fill(selector, text)
    protected suspend fun pressSequentially(selector: String, text: String) = page.locator(selector).pressSequentially(text)
    protected suspend fun exists(selector: String) = page.querySelector(selector) != null
    protected suspend fun getText(selector: String) = page.textContent(selector) ?: ""
    protected suspend fun waitForSelector(selector: String) = page.waitForSelector(selector)
    protected suspend fun waitForURL(url: String) = page.waitForURL(url)

    // 扩展的页面操作
    protected suspend fun querySelectorAll(selector: String) = page.querySelectorAll(selector)
    protected suspend fun press(selector: String, key: String) = page.press(selector, key)
    protected suspend fun hover(selector: String) = page.hover(selector)

    // 安全的页面操作（带重试和错误处理）
    protected suspend fun safeClick(selector: String, maxRetries: Int = config.retryTimes) {
        repeat(maxRetries) { attempt ->
            try {
                waitForSelector(selector)
                click(selector)
                return
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(config.retryDelay * (attempt + 1))
            }
        }
    }

    protected suspend fun safeFill(selector: String, text: String, maxRetries: Int = config.retryTimes) {
        repeat(maxRetries) { attempt ->
            try {
                waitForSelector(selector)
                fill(selector, text)
                return
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(config.retryDelay * (attempt + 1))
            }
        }
    }

    protected suspend fun safePressSequentially(selector: String, text: String, maxRetries: Int = config.retryTimes) {
        repeat(maxRetries) { attempt ->
            try {
                waitForSelector(selector)
                pressSequentially(selector, text)
                return
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                delay(config.retryDelay * (attempt + 1))
            }
        }
    }

    // 业务相关的抽象方法
    protected abstract suspend fun isLoggedIn(): Boolean
    protected abstract suspend fun search(keywords: List<String>)
    protected abstract suspend fun submitResume()

    // 等待登录完成
    protected open suspend fun waitForLogin() {
        navigate(loginUrl)
        viewModel.addLog("请在打开的浏览器中完成登录")
        var waitTime = 0
        while (!isLoggedIn()) {
            delay(1000) // 每秒检查一次
            waitTime++
            if (waitTime >= config.loginTimeout) { // 使用配置中的登录超时时间
                throw Exception("登录超时，请重试")
            }
        }
        viewModel.addLog("登录成功，开始执行任务")
    }

    // 对外的主要方法
    suspend fun start(keywords: List<String>) {
        try {
            init()
            waitForLogin()
            search(keywords)
            submitResume() // Boss直聘的实现中此方法可能为空
            viewModel.addLog("任务完成，共投递 $submittedCount 份简历")
        } catch (e: Exception) {
            viewModel.addLog("发生错误：${e.message}")
            //  e.printStackTrace() // 调试时可打印完整堆栈
            throw e // 如果上层调用需要处理，则重新抛出
        } finally {
            shutdown()
        }
    }

    fun shutdown() {
        try {
            // 清理资源，注意检查是否已初始化，避免 NullPointerException
            if (::page.isInitialized) page.close()
            if (::context.isInitialized) context.close()
            if (::browser.isInitialized) browser.close()
        } catch (e: Exception) {
            viewModel.addLog("关闭浏览器时发生错误：${e.message}")
            // e.printStackTrace() // 调试时可打印完整堆栈
        }
    }
}