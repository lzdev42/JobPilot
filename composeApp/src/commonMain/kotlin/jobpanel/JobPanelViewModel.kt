package jobpanel

import ai.AiManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logview.LogViewModel
import playwright.*
import utils.AppConfig

enum class SeekerType{
    BOSS,JOB51,LIEPIN
}

data class BossFilter(
    val jobType: String = "",
    val experience: String = "",
    val degree: String = "",
    val scale: String = "",
    val city: String = "",
    val salary: String = ""
)

class JobPanelViewModel(val seekerType: SeekerType): ViewModel() {
    val logViewModel = LogViewModel()
    var bossSeeker: BossJobSeeker? = null
    var job51Seeker: BossJobSeeker? = null
    var liepinSeeker: BossJobSeeker? = null
    // 关键词状态
    private var _keywords = mutableStateListOf<String>()
    val keywords: List<String> = _keywords

    // Boss配置选项状态
    var bossFilter by mutableStateOf(BossFilter())

    fun updateKeywords(newKeywords: List<String>) {
        _keywords.clear()
        _keywords.addAll(newKeywords)
    }

    // 是否正在运行
    var isRunning by mutableStateOf(false)
        private set


    fun startJobSeeker() {
        if (isRunning) {
            isRunning = false
            when (seekerType) {
                SeekerType.BOSS -> {
                    bossSeeker?.shutdown()
                    bossSeeker = null
                    logViewModel.addLog("停止Boss直聘任务...")
                }
                SeekerType.JOB51 -> {
                    job51Seeker?.shutdown()
                    logViewModel.addLog("停止51Job任务...")
                }
                SeekerType.LIEPIN -> {
                    liepinSeeker?.shutdown()
                    logViewModel.addLog("停止猎聘任务...")
                }
            }
            return
        }
        when {
            AppConfig.prompt.isEmpty() -> {
                logViewModel.addLog("请先设置提示词")
                return
            }
            AppConfig.resume.isEmpty() -> {
                logViewModel.addLog("请先设置简历")
                return
            }
            AppConfig.geminiAppKey.isEmpty() -> {
                logViewModel.addLog("请先设置Gemini应用密钥")
                return
            }
        }

        AiManager.initGemini()

        val searchCriteria = SearchCriteria(
            keywords = keywords.ifEmpty { 
                listOf(
                    "C#",
                    ".NET",
                    "自动化测试",
                    "软件工程师",
                    "高级软件工程师",
                    "跨平台开发",
                    "全栈工程师"
                )
            },
            cities = if (seekerType == SeekerType.BOSS && bossFilter.city.isNotEmpty()) {
                listOf(bossFilter.city)
            } else {
                listOf("101190400", "101190200", "101020100")
            },
            experience = if (seekerType == SeekerType.BOSS) bossFilter.experience else "0",
            degree = if (seekerType == SeekerType.BOSS) bossFilter.degree else "0",
            salary = if (seekerType == SeekerType.BOSS) bossFilter.salary else "0",
            jobType = if (seekerType == SeekerType.BOSS) bossFilter.jobType else "0",
            scale = if (seekerType == SeekerType.BOSS) bossFilter.scale else "0",
            stage = "0"
        )
        val config = PlaywrightConfig(
            searchCriteria = searchCriteria,
            debug = false
        )
        isRunning = true
        println("isRunning: $isRunning")
        when (seekerType) {
            SeekerType.BOSS -> {
                // Start BOSS job seeker
                logViewModel.addLog("启动Boss直聘...")
                bossSeeker = null
                bossSeeker = BossJobSeeker(logViewModel, config)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        bossSeeker?.start(config.searchCriteria.keywords)
                    } catch (e: Exception) {
                        logViewModel.addLog("启动Boss直聘失败: ${e.message}")
                    }finally {
                        println("BossSeeker isRunning: ${isRunning}")
                    }
                }
            }
            SeekerType.JOB51 -> {
                // Start QCWY job seeker
                logViewModel.addLog("启动51Job...")
            }
            SeekerType.LIEPIN -> {
                // Start LIEPIN job seeker
                logViewModel.addLog("启动猎聘...")
            }
        }
    }
}