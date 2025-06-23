package utils

import java.io.File

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths

@Serializable
data class UserSettings(
    var geminiAppKey: String = "",
    var resume: String = "",
    var prompt: String = ""
)

@Serializable
data class BlackList(
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
    private const val BLACK_LIST_FILE_NAME = "blacklist.json"

    // 缓存的配置对象
    private var _settings: UserSettings? = null
    private var _blackList: BlackList? = null
    private var _bossConfig: BossConfig? = null

    // 用于JSON序列化和反序列化
    private val jsonFormat = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // 懒加载配置
    private val settings: UserSettings
        get() {
            if (_settings == null) {
                _settings = loadOrCreateConfig<UserSettings>(getUserConfigFile()) { UserSettings() }
            }
            return _settings!!
        }

    // 懒加载黑名单
    private val blackList: BlackList
        get() {
            if (_blackList == null) {
                _blackList = loadOrCreateConfig<BlackList>(getBlackListFile()) { BlackList() }
            }
            return _blackList!!
        }

    // 提供直接的属性访问
    var resume: String
        get() = settings.resume
        set(value) {
            settings.resume = value
            saveConfig(settings, getUserConfigFile())
        }

    var prompt: String
        get() = settings.prompt
        set(value) {
            settings.prompt = value
            saveConfig(settings, getUserConfigFile())
        }

    var geminiAppKey: String
        get() = settings.geminiAppKey
        set(value) {
            settings.geminiAppKey = value
            saveConfig(settings, getUserConfigFile())
        }

    /**
     * 获取完整的用户配置对象（只读访问）
     */
    fun getUserSettings(): UserSettings {
        return settings.copy() // 返回副本，防止外部直接修改
    }

    /**
     * 获取黑名单配置（只读访问）
     */
    fun getBlackListCopy(): BlackList {
        return blackList.copy() // 返回副本，防止外部直接修改
    }

    /**
     * 批量更新配置
     */
    fun updateSettings(block: UserSettings.() -> Unit) {
        settings.apply(block)
        saveConfig(settings, getUserConfigFile())
    }

    /**
     * 更新黑名单
     */
    fun updateBlackList(companies: List<String>) {
        _blackList = BlackList(companies)
        saveConfig(_blackList!!, getBlackListFile())
    }

    /**
     * 添加公司到黑名单
     */
    fun addToBlackList(company: String) {
        val currentCompanies = blackList.companies.toMutableList()
        if (!currentCompanies.contains(company)) {
            currentCompanies.add(company)
            updateBlackList(currentCompanies)
        }
    }

    /**
     * 从黑名单移除公司
     */
    fun removeFromBlackList(company: String) {
        val currentCompanies = blackList.companies.toMutableList()
        if (currentCompanies.remove(company)) {
            updateBlackList(currentCompanies)
        }
    }

    /**
     * 检查公司是否在黑名单中
     */
    fun isInBlackList(company: String): Boolean {
        return blackList.companies.contains(company)
    }

    /**
     * 重新加载配置（从文件）
     */
    fun reloadSettings() {
        _settings = null // 清除缓存，强制重新加载
    }

    /**
     * 重新加载黑名单（从文件）
     */
    fun reloadBlackList() {
        _blackList = null // 清除缓存，强制重新加载
    }

    /**
     * 重新加载所有配置
     */
    fun reloadAllConfigs() {
        reloadSettings()
        reloadBlackList()
    }

    fun getFilledPrompt(): String {
        val placeholderStart = "[用户输入的用户简介开始]\n---"
        val placeholderEnd = "---\n[用户输入的用户简介结束]"

        // 更精确地定位替换区域，避免意外替换其他地方的 "---"
        val startIndex = prompt.indexOf(placeholderStart)
        val endIndex = prompt.indexOf(placeholderEnd, startIndex + placeholderStart.length)

        if (startIndex != -1 && endIndex != -1) {
            // 提取占位符开始标记之后，和占位符结束标记之前的部分
            // 即 "---" 和 "---" 中间的部分
            val prefix = prompt.substring(0, startIndex + placeholderStart.length)
            val suffix = prompt.substring(endIndex) // endIndex 指向 "---" 的开头

            return "$prefix\n${this.resume}\n$suffix"
        }

        // 如果未找到标记，返回原始prompt或抛出异常，这里选择返回原始prompt
        // 你可以根据需要调整此行为
        return prompt
    }

    /**
     * 获取 bossconfig.json 文件的路径
     * @return 如果文件存在返回文件路径，如果不存在返回 null
     */
    fun getBossConfigPath(): String? {
        // 尝试从资源文件中获取
        val resourcePath = "files/bossconfig.json"
        val resourceUrl = AppConfig::class.java.classLoader.getResource(resourcePath)

        if (resourceUrl != null) {
            return resourceUrl.path
        }

        // 如果从资源中没找到，尝试从文件系统中查找
        val possiblePaths = listOf(
            "composeResources/files/bossconfig.json",
            "src/desktopMain/composeResources/files/bossconfig.json",
            "files/bossconfig.json"
        )

        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                return file.absolutePath
            }
        }

        println("无法找到 bossconfig.json 文件，已尝试以下路径：")
        println("- 资源路径: $resourcePath")
        possiblePaths.forEach { println("- 文件系统路径: $it") }

        return null
    }

    /**
     * 获取应用程序的用户特定配置目录。
     */
    private fun getUserConfigDirectory(): File {
        val userHome = System.getProperty("user.home")
        val osName = System.getProperty("os.name").lowercase()

        val path = when {
            osName.contains("win") -> Paths.get(System.getenv("APPDATA"), APP_NAME)
            osName.contains("mac") -> Paths.get(userHome, "Library", "Application Support", APP_NAME)
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> Paths.get(userHome, ".config", APP_NAME)
            else -> Paths.get(userHome, ".${APP_NAME.lowercase()}")
        }

        val dir = path.toFile()
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath())
                println("用户配置目录已创建: ${dir.absolutePath}")
            } catch (e: Exception) {
                System.err.println("错误：无法创建用户配置目录 ${dir.absolutePath}: ${e.message}")
                throw IllegalStateException("无法创建用户配置目录", e)
            }
        }
        return dir
    }

    /**
     * 获取用户配置文件的完整路径。
     */
    private fun getUserConfigFile(): File {
        return File(getUserConfigDirectory(), CONFIG_FILE_NAME)
    }

    /**
     * 获取黑名单文件的完整路径。
     */
    private fun getBlackListFile(): File {
        return File(getUserConfigDirectory(), BLACK_LIST_FILE_NAME)
    }

    /**
     * 通用的配置加载方法
     */
    private inline fun <reified T> loadOrCreateConfig(configFile: File, defaultProvider: () -> T): T {
        if (configFile.exists() && configFile.isFile) {
            try {
                val jsonString = configFile.readText()
                if (jsonString.isNotBlank()) {
                    val config = jsonFormat.decodeFromString<T>(jsonString)
                    println("配置已从 ${configFile.absolutePath} 加载")
                    return config
                } else {
                    println("配置文件 ${configFile.absolutePath} 为空，将创建默认配置。")
                }
            } catch (e: Exception) {
                System.err.println("错误：无法从 ${configFile.absolutePath} 读取或解析配置: ${e.message}")
                println("将创建一个新的默认配置文件。旧文件（如果损坏）将被覆盖。")
            }
        } else {
            println("配置文件 ${configFile.absolutePath} 未找到，将创建新的默认配置。")
        }

        // 创建并保存默认配置
        val defaultConfig = defaultProvider()
        saveConfig(defaultConfig, configFile)
        return defaultConfig
    }

    /**
     * 通用的配置保存方法
     */
    private inline fun <reified T> saveConfig(config: T, configFile: File) {
        try {
            val jsonString = jsonFormat.encodeToString(config)
            configFile.writeText(jsonString)
            println("配置已保存到: ${configFile.absolutePath}")
            //reloadConfig()
        } catch (e: Exception) {
            System.err.println("错误：无法保存配置到 ${configFile.absolutePath}: ${e.message}")
        }
    }

//    private fun reloadConfig(){
//        _settings = loadOrCreateConfig<UserSettings>(getUserConfigFile()) { UserSettings() }
//    }

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
}