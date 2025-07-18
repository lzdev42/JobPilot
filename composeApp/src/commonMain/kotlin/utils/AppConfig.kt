package utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Serializable
data class UserSettings(
    var geminiAppKey: String = "",
    var resume: String = "",
    var prompt: String = ""
)

@Serializable
data class Blacklist( // 使用包装类以支持未来扩展和类型安全的JSON结构
    val companies: List<String> = emptyList()
)

@Serializable
data class BossConfig(
    val jobType: Map<String, String> = emptyMap(),
    val experience: Map<String, String> = emptyMap(),
    val degree: Map<String, String> = emptyMap(),
    val scale: Map<String, String> = emptyMap(),
    val city: Map<String, String> = emptyMap(),
    val salary: Map<String, String> = emptyMap()
)

object AppConfig {
    private const val APP_NAME = "JobPilot"
    private const val CONFIG_FILE_NAME = "config.json"
    private const val BLACKLIST_FILE_NAME = "blacklist.json"
    private const val SUBMITTED_JOBS_FILE_NAME = "submitted_history.json"

    // 缓存的配置对象
    private var _settings: UserSettings? = null
    private var _bossConfig: BossConfig? = null

    private val jsonFormat = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // 用户设置的懒加载属性
    private val settings: UserSettings
        get() {
            if (_settings == null) {
                _settings = loadOrCreateConfig(getUserConfigFile()) { UserSettings() }
            }
            return _settings!!
        }

    // 公开的属性，用于UI绑定（只更新内存缓存）
    var resume: String
        get() = settings.resume
        set(value) { settings.resume = value }

    var prompt: String
        get() = settings.prompt
        set(value) { settings.prompt = value }

    var geminiAppKey: String
        get() = settings.geminiAppKey
        set(value) { settings.geminiAppKey = value }

    /**
     * 显式地将当前用户设置保存到文件。
     */
    fun saveUserSettings() {
        saveConfig(settings, getUserConfigFile())
    }

    /**
     * 从文件加载黑名单公司列表。
     */
    fun loadBlacklist(): List<String> {
        val blacklistFile = getBlacklistFile()
        return loadOrCreateConfig<Blacklist>(blacklistFile) { Blacklist() }.companies
    }

    /**
     * 将整个黑名单公司列表保存到文件。
     */
    fun saveBlacklist(companies: List<String>) {
        saveConfig(Blacklist(companies), getBlacklistFile())
    }

    /**
     * 从文件加载已投递职位列表。
     */
    fun loadSubmittedJobs(): List<SubmittedJob> {
        val submittedJobsFile = getSubmittedJobsFile()
        return loadOrCreateConfig<List<SubmittedJob>>(submittedJobsFile) { emptyList() }
    }

    /**
     * 将整个已投递职位列表保存到文件。
     */
    fun saveSubmittedJobs(jobs: List<SubmittedJob>) {
        saveConfig(jobs, getSubmittedJobsFile())
    }

    /**
     * 通过清除缓存从文件重新加载所有配置。
     * 下一次访问将触发重新加载。
     */
    fun reloadAllConfigs() {
        _settings = null
        _bossConfig = null
        // 业务逻辑层（如ApplyCheck）有责任重新加载自己的数据。
    }

    fun getFilledPrompt(): String {
        val placeholderStart = "[用户输入的用户简介开始]\n---"
        val placeholderEnd = "---\n[用户输入的用户简介结束]"

        val startIndex = prompt.indexOf(placeholderStart)
        val endIndex = prompt.indexOf(placeholderEnd, startIndex + placeholderStart.length)

        if (startIndex != -1 && endIndex != -1) {
            val prefix = prompt.substring(0, startIndex + placeholderStart.length)
            val suffix = prompt.substring(endIndex)
            return "$prefix\n${this.resume}\n$suffix"
        }
        return prompt
    }

    fun getBossConfig(): BossConfig? {
        if (_bossConfig == null) {
            val configPath = getBossConfigPath()
            if (configPath != null) {
                try {
                    val jsonString = File(configPath).readText()
                    _bossConfig = jsonFormat.decodeFromString<BossConfig>(jsonString)
                } catch (e: Exception) {
                    System.err.println("无法读取 Boss 配置: ${e.message}")
                }
            }
        }
        return _bossConfig
    }

    private fun getBossConfigPath(): String? {
        val resourcePath = "files/bossconfig.json"
        val resourceUrl = AppConfig::class.java.classLoader.getResource(resourcePath)
        if (resourceUrl != null) return resourceUrl.path

        val possiblePaths = listOf(
            "composeResources/files/bossconfig.json",
            "src/desktopMain/composeResources/files/bossconfig.json",
            "files/bossconfig.json"
        )
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) return file.absolutePath
        }
        return null
    }

    private fun getUserConfigDirectory(): File {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()

        val path = when {
            osName.contains("win") -> Paths.get(System.getenv("APPDATA"), APP_NAME)
            osName.contains("mac") -> Paths.get(userHome, "Library", "Application Support", APP_NAME)
            osName.contains("nix") || osName.contains("nux") -> Paths.get(userHome, ".config", APP_NAME)
            else -> Paths.get(userHome, ".${APP_NAME.lowercase()}")
        }

        val dir = path.toFile()
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath())
                println("用户配置目录已创建: ${dir.absolutePath}")
            } catch (e: Exception) {
                throw IllegalStateException("无法创建用户配置目录", e)
            }
        }
        return dir
    }

    private fun getUserConfigFile(): File = File(getUserConfigDirectory(), CONFIG_FILE_NAME)
    private fun getBlacklistFile(): File = File(getUserConfigDirectory(), BLACKLIST_FILE_NAME)
    private fun getSubmittedJobsFile(): File = File(getUserConfigDirectory(), SUBMITTED_JOBS_FILE_NAME)

    private inline fun <reified T> loadOrCreateConfig(configFile: File, defaultProvider: () -> T): T {
        if (configFile.exists() && configFile.isFile) {
            try {
                val jsonString = configFile.readText()
                if (jsonString.isNotBlank()) {
                    return jsonFormat.decodeFromString<T>(jsonString)
                }
            } catch (e: Exception) {
                System.err.println("错误：无法从 ${configFile.absolutePath} 读取或解析配置: ${e.message}")
            }
        }
        val defaultConfig = defaultProvider()
        saveConfig(defaultConfig, configFile)
        return defaultConfig
    }

    private inline fun <reified T> saveConfig(config: T, configFile: File) {
        try {
            val jsonString = jsonFormat.encodeToString(config)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            System.err.println("错误：无法保存配置到 ${configFile.absolutePath}: ${e.message}")
        }
    }
}