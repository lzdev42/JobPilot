package utils

import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class SubmittedJob(
    val companyName: String,
    val jobTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ApplyCheck {

    private var blacklist: MutableList<String> = mutableListOf()
    private var submittedJobs: MutableList<SubmittedJob> = mutableListOf()

    private const val COOL_DOWN_DAYS = 15L // 冷却时间设为15天

    init {
        loadData()
    }

    private fun loadData() {
        blacklist = AppConfig.loadBlacklist().toMutableList()
        submittedJobs = AppConfig.loadSubmittedJobs().toMutableList()
    }

    fun canProceed(companyName: String, jobTitle: String): Boolean {
        // 1. 检查黑名单
        if (blacklist.any { it.equals(companyName, ignoreCase = true) }) {
            return false
        }

        // 2. 检查投递历史
        val previouslySubmitted = submittedJobs.find {
            it.companyName.equals(companyName, ignoreCase = true) && it.jobTitle.equals(jobTitle, ignoreCase = true)
        }

        if (previouslySubmitted == null) {
            // 之前未投递过，可以继续
            return true
        }

        // 3. 检查15天的“冷静期”
        val daysSinceSubmission = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - previouslySubmitted.timestamp)
        return daysSinceSubmission > COOL_DOWN_DAYS
    }

    fun recordSubmission(companyName: String, jobTitle: String) {
        // 移除任何针对同一职位的旧记录，以更新时间戳
        submittedJobs.removeAll {
            it.companyName.equals(companyName, ignoreCase = true) && it.jobTitle.equals(jobTitle, ignoreCase = true)
        }
        // 添加新记录
        submittedJobs.add(SubmittedJob(companyName = companyName, jobTitle = jobTitle))
        AppConfig.saveSubmittedJobs(submittedJobs)
        reload()
    }

    fun addToBlacklist(companyName: String) {
        if (blacklist.none { it.equals(companyName, ignoreCase = true) }) {
            blacklist.add(companyName)
            AppConfig.saveBlacklist(blacklist)
            reload()
        }
    }

    /**
     * 从文件重新加载数据到内存
     */
    fun reload() {
        loadData()
    }
}