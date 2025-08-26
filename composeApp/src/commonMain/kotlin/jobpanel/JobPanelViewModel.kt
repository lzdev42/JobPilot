package jobpanel

import ai.AiManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    // 关键词状态 (注释掉原有的多关键词方式)
    // private var _keywords = mutableStateListOf<String>()
    // val keywords: List<String> = _keywords
    
    // fun updateKeywords(newKeywords: List<String>) {
    //     _keywords.clear()
    //     _keywords.addAll(newKeywords)
    // }
    
    // 改为单个关键词输入
    var singleKeyword by mutableStateOf("")
        private set
    
    fun updateSingleKeyword(keyword: String) {
        singleKeyword = keyword
    }
    
    // 兼容原有接口，将单个关键词转为列表
    val keywords: List<String>
        get() = if (singleKeyword.isBlank()) emptyList() else listOf(singleKeyword.trim())

    // Boss配置选项状态
    var bossFilter by mutableStateOf(BossFilter())

    // 是否正在运行
    var isRunning by mutableStateOf(false)
        private set
    
    // 当前运行的协程Job
    private var currentJob: Job? = null


    fun startJobSeeker() {
        if (isRunning) {
            // 停止当前任务
            logViewModel.addLog("正在停止任务...")
            isRunning = false
            
            // 取消当前协程
            currentJob?.cancel()
            currentJob = null
            
            // 关闭浏览器资源
            when (seekerType) {
                SeekerType.BOSS -> {
                    bossSeeker?.shutdown()
                    bossSeeker = null
                    logViewModel.addLog("已停止Boss直聘任务")
                }
                SeekerType.JOB51 -> {
                    job51Seeker?.shutdown()
                    logViewModel.addLog("已停止51Job任务")
                }
                SeekerType.LIEPIN -> {
                    liepinSeeker?.shutdown()
                    logViewModel.addLog("已停止猎聘任务")
                }
            }
            return
        }
        when {
            singleKeyword.isBlank() -> {
                logViewModel.addLog("请先输入搜索关键词")
                return
            }
            AppConfig.prompt.isEmpty() -> {
                logViewModel.addLog("请先设置提示词")
                return
            }
            AppConfig.resume.isEmpty() -> {
                logViewModel.addLog("请先设置简历")
                return
            }
            AppConfig.rejectionRules.isEmpty() -> {
                logViewModel.addLog("请先设置求职红线")
                return
            }
            AppConfig.preferenceRules.isEmpty() -> {
                logViewModel.addLog("请先设置求职偏好")
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
                // 注释掉硬编码的默认关键词，要求用户必须输入
                // listOf(
                //     "C#",
                //     ".NET",
                //     "自动化测试",
                //     "软件工程师",
                //     "高级软件工程师",
                //     "跨平台开发",
                //     "全栈工程师"
                // )
                emptyList()
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
            debug = AppConfig.isDebugMode
        )
        isRunning = true
        if (AppConfig.isDebugMode) {
            logViewModel.addLog("[DEBUG] isRunning状态: $isRunning")
        }
        when (seekerType) {
            SeekerType.BOSS -> {
                // Start BOSS job seeker
                logViewModel.addLog("启动Boss直聘...")
                bossSeeker = null
                bossSeeker = BossJobSeeker(logViewModel, config) { !isRunning }
                currentJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        bossSeeker?.start(config.searchCriteria.keywords)
                        logViewModel.addLog("Boss直聘任务正常完成")
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            logViewModel.addLog("Boss直聘任务已被取消")
                        } else {
                            logViewModel.addLog("Boss直聘任务失败: ${e.message}")
                        }
                    } finally {
                        isRunning = false
                        currentJob = null
                        bossSeeker?.shutdown()
                        bossSeeker = null
                        if (AppConfig.isDebugMode) {
                            logViewModel.addLog("[DEBUG] BossSeeker任务结束，isRunning: $isRunning")
                        }
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