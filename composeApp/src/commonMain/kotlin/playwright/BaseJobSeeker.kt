package playwright

import com.microsoft.playwright.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext // 协程上下文切换
import logview.LogViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Paths

abstract class BaseJobSeeker(
    protected val viewModel: LogViewModel,
    protected val config: PlaywrightConfig
) {
    protected lateinit var browser: Browser
    protected lateinit var context: BrowserContext
    protected lateinit var page: Page

    protected var submittedCount: Int = 0
    protected abstract val loginUrl: String
    protected abstract val homeUrl: String

    // 查找 Playwright JAR 包的路径，用于后续的 CLI 调用
    private suspend fun findPlaywrightJarPath(): String? = withContext(Dispatchers.IO) {
        // 尝试通过 Playwright CLI 类的 CodeSource 来定位 JAR。
        // 注意：这种定位方式在不同打包和运行环境下可能需要调整。
        try {
            val cliClass = Class.forName("com.microsoft.playwright.CLI")
            val protectionDomain = cliClass.protectionDomain
            val codeSource = protectionDomain.codeSource
            if (codeSource != null) {
                val location = codeSource.location
                if (location != null) {
                    var path = Paths.get(location.toURI()).toString()
                    // 检查路径是否指向一个有效的 Playwright JAR (非驱动包)
                    if (path.contains("playwright") && path.endsWith(".jar") && !path.contains("playwright-driver-bundle")) {
                        return@withContext path
                    }
                    // 如果上面的方法不行 (比如在IDE中是classes目录)，尝试扫描整个 classpath
                    val classpath = System.getProperty("java.class.path")
                    val classpathEntries = classpath.split(File.pathSeparatorChar)
                    // 优先找不含 "-driver-bundle" 和 "playwright-cli" (有时cli是单独的小jar) 的主库
                    return@withContext classpathEntries.firstOrNull {
                        it.contains("playwright") && it.endsWith(".jar") && !it.contains("-driver-bundle") && !it.contains("playwright-cli")
                    } ?: classpathEntries.firstOrNull { // 如果没找到，放宽条件再找一次
                        it.contains("playwright") && it.endsWith(".jar")
                    }
                }
            }
        } catch (e: Exception) {
            viewModel.addLog("查找 Playwright JAR 时出错: ${e.message}")
            //  e.printStackTrace() // 调试时可以打开，帮助定位问题
        }
        return@withContext null // 未找到JAR
    }

    // 准备 Chromium 环境并记录相关日志
    private suspend fun prepareChromiumAndLog() {
        viewModel.addLog("开始准备 Chromium 并记录日志...")

        val playwrightJarPath = findPlaywrightJarPath()
        if (playwrightJarPath == null) {
            viewModel.addLog("[错误] 未能定位到 Playwright JAR 文件。")
            viewModel.addLog("将无法通过 Playwright CLI 准备 Chromium，相关日志不会被捕获。")
            viewModel.addLog("Playwright 将尝试其默认的浏览器设置流程，请留意控制台输出。")
            return // JAR未找到，后续让Playwright自行处理
        }

        viewModel.addLog("找到 Playwright JAR: $playwrightJarPath")
        viewModel.addLog("准备执行 Playwright CLI: install chromium...")

        try {
            // 在IO线程中执行外部进程并捕获输出
            withContext(Dispatchers.IO) {
                val javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
                val processBuilder = ProcessBuilder(
                    javaExecutable,                 // Java 可执行文件路径
                    "-cp",
                    playwrightJarPath,              // Playwright JAR 的 classpath
                    "com.microsoft.playwright.CLI", // Playwright CLI 的主类
                    "install",                      // CLI 命令：安装
                    "chromium"                      // CLI 参数：指定 chromium
                )
                processBuilder.redirectErrorStream(true) // 合并标准输出和标准错误流

                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    viewModel.addLog("[CLI 日志] $line") // 将CLI的每行输出添加到应用日志
                }
                val exitCode = process.waitFor() // 等待命令执行完成
                if (exitCode == 0) {
                    viewModel.addLog("Playwright CLI 'install chromium' 命令执行成功。")
                } else {
                    viewModel.addLog("[错误] Playwright CLI 'install chromium' 命令执行失败，退出码: $exitCode。请检查日志详情。")
                }
            }
        } catch (e: Exception) {
            viewModel.addLog("[错误] 执行 'install chromium' CLI 命令时发生异常: ${e.message}")
            // e.printStackTrace() // 调试时可以打开
        }
    }

    // 初始化浏览器环境
    protected suspend fun init() {
        // 首先尝试准备 Chromium 并捕获其安装/准备日志
        prepareChromiumAndLog()

        viewModel.addLog("开始初始化 Playwright 主实例...")
        val playwright = Playwright.create() // 创建 Playwright 实例
        viewModel.addLog("Playwright 主实例初始化完成。准备启动 Chromium 浏览器...")

        // 设置浏览器启动参数，使其在后台运行
        val args = listOf(
            "--window-position=0,0",  // 窗口位置
            "--window-size=1920,1080", // 窗口大小
            "--auto-open-devtools-for-tabs=false", // 不自动打开开发者工具
            "--disable-popup-blocking", // 禁用弹窗阻止
            "--disable-notifications", // 禁用通知
            "--noerrdialogs", // 禁用错误对话框
            "--disable-session-crashed-bubble" // 禁用会话崩溃气泡
        )
        
        browser = playwright.chromium().launch( // 启动 Chromium
            BrowserType.LaunchOptions()
                .setHeadless(config.headless)
                .setArgs(args)
        )
        viewModel.addLog("Chromium 浏览器已启动。准备创建新的浏览器上下文...")

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