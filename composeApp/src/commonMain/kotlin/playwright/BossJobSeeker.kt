package playwright

import ai.AiManager
import ai.AiProvider
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.LoadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import logview.LogViewModel
import utils.AppConfig
import utils.ApplyCheck
import utils.getGreeting
import kotlin.random.Random

class BossJobSeeker(
    viewModel: LogViewModel,
    config: PlaywrightConfig
) : BaseJobSeeker(viewModel, config) {

    override val loginUrl = "https://www.zhipin.com/web/user/?ka=header-login"
    override val homeUrl = "https://www.zhipin.com"

    // 投递频率控制
    private data class RateLimit(
        val minInterval: Long = 3000, // 最小间隔3秒
        val maxInterval: Long = 10000, // 最大间隔10秒
        val pageInterval: Long = Random.nextLong(3000, 10000),  // 随机3到10秒
        val maxPerHour: Int = 50,       // 每小时最大投递数
        val maxPerDay: Int = 300        // 每天最大投递数
    )

    private val rateLimit = RateLimit()
    private var hourlyApplyCount = 0
    private var dailyApplyCount = 0
    private var lastApplyTime = 0L
    private var hourStartTime = System.currentTimeMillis()

    companion object {
        private object Selectors {
            // 登录相关
            const val LOGIN_SCAN_SWITCH = "text=APP扫码登录"
            const val LOGIN_SUCCESS_HEADER = ".user-nav"
            const val QR_CODE_IMAGE = "img[src^='/wapi/zpweixin/qrcode/getqrcode']"

            // 职位列表相关
            const val JOB_LIST_CONTAINER = "//div[@class='job-list-container']"
            const val JOB_CARD = "li.job-card-box"
            const val JOB_NAME = "a.job-name"
            const val COMPANY_NAME = "span.boss-name"
            const val JOB_AREA = "span.company-location"
            const val JOB_DESCRIPTION = ".job-detail-section:has(h3:text('职位描述')) .job-sec-text"

            // 聊天相关
            const val SEND_BUTTON = ".send-message"
            const val CHAT_INPUT = ".input-area"
            // 错误提示
            const val ERROR_MESSAGE = ".dialog-con"
            const val LIMIT_MESSAGE = "text=已达上限"
            const val HR_ACTIVE_TIME = ".job-boss-info .boss-active-time"
            const val DEAD_HR_TEXT = "text=/.*日前活跃.*/"

        }
    }

    override suspend fun isLoggedIn(): Boolean {
        println("--> [isLoggedIn] 开始检查登录状态...")
        val result = try {
            exists(Selectors.LOGIN_SUCCESS_HEADER)
        } catch (e: Exception) {
            false
        }
        println("<-- [isLoggedIn] 登录状态: $result")
        return result
    }

    /**
     * 获取登录页面的二维码URL
     * @return 完整的二维码图片URL，如果找不到二维码则返回null
     */
    suspend fun getQrCodeUrl(): String? {
        println("--> [getQrCodeUrl] 开始获取二维码URL...")
        try {
            println("    [getQrCodeUrl] 等待二维码图片元素 '${Selectors.QR_CODE_IMAGE}' 出现...")
            waitForSelector(Selectors.QR_CODE_IMAGE)

            val relativePath = page.locator(Selectors.QR_CODE_IMAGE).getAttribute("src")
            println("    [getQrCodeUrl] 获取到二维码 'src' 属性: $relativePath")
            if (relativePath != null) {
                val fullQrCodeUrl = "https://www.zhipin.com" + relativePath
                viewModel.addLog("获取到二维码URL: $fullQrCodeUrl")
                println("<-- [getQrCodeUrl] 成功构建二维码URL: $fullQrCodeUrl")
                return fullQrCodeUrl
            } else {
                println("    [getQrCodeUrl] 未能获取到二维码 'src' 属性")
            }
        } catch (e: Exception) {
            viewModel.addLog("获取二维码URL失败: ${e.message}")
            println("<-- [getQrCodeUrl] 获取二维码URL时发生异常: ${e.message}")
        }
        return null
    }

    override suspend fun waitForLogin() {
        println("--> [waitForLogin] 开始等待用户登录...")
        navigate(loginUrl)
        viewModel.addLog("请在打开的浏览器中完成登录")

        try {
            delay(2000)
            println("    [waitForLogin] 检查是否存在 'APP扫码登录' 切换按钮...")
            if (exists(Selectors.LOGIN_SCAN_SWITCH)) {
                viewModel.addLog("正在切换到APP扫码登录模式...")
                click(Selectors.LOGIN_SCAN_SWITCH)
                println("    [waitForLogin] 已点击 'APP扫码登录' 按钮")
                viewModel.addLog("已切换到APP扫码登录模式，请使用手机APP扫描二维码登录")

                val qrCodeUrl = getQrCodeUrl()
                if (qrCodeUrl != null) {
                    viewModel.addLog("二维码URL: $qrCodeUrl")
                }
            }
        } catch (e: Exception) {
            viewModel.addLog("切换到扫码登录模式失败: ${e.message}，请手动切换到扫码登录")
        }

        println("    [waitForLogin] 进入循环，每秒检查一次登录状态...")
        var waitTime = 0
        while (!isLoggedIn()) {
            delay(1000)
            waitTime++
            println("    [waitForLogin] 等待中... (${waitTime}s / ${config.loginTimeout}s)")
            if (waitTime >= config.loginTimeout) {
                println("<-- [waitForLogin] 登录超时！")
                throw Exception("登录超时，请重试")
            }
        }
        println("<-- [waitForLogin] 检测到登录成功！")
        viewModel.addLog("登录成功，开始执行任务")
    }

    override suspend fun search(keywords: List<String>) {
        println("--> [search] 开始执行总的搜索任务...")
        for (city in config.searchCriteria.cities) {
            println("==> [search] 开始处理城市: $city")
            for (keyword in keywords) {
                println("===> [search] 开始处理关键词: '$keyword'")
                try {
                    println("     [search] 即将调用 searchAndApply ...")
                    searchAndApply(keyword, city)
                } catch (e: Exception) {
                    viewModel.addLog("搜索关键词 $keyword 在城市 $city 时发生错误: ${e.message}")
                    println("====> [search] 捕获到错误，关键词'$keyword'在城市'$city'的搜索中断: ${e.message}")
                    delay(rateLimit.pageInterval)
                }
            }
        }
    }

    private suspend fun searchAndApply(keyword: String, city: String) {
        println("--> [searchAndApply] 开始搜索并投递，关键词: '$keyword', 城市: '$city'")

        val searchUrl = buildSearchUrl(keyword, city)
        navigate(searchUrl)
        println("    [searchAndApply] 已导航至搜索页，等待列表加载...")
        waitForSelector(Selectors.JOB_LIST_CONTAINER)

        println("    [searchAndApply] 开始滚动页面以加载更多职位...")
        var previousCount = 0
        var unchangedCount = 0
        while (unchangedCount < 2) {
            val currentCount = page.locator(Selectors.JOB_CARD).count()
            println("    [searchAndApply] 滚动前，当前职位数: $currentCount")
            if (currentCount > previousCount) {
                previousCount = currentCount
                unchangedCount = 0
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
                delay(2000)
            } else {
                unchangedCount++
            }
        }

        val jobCards = page.locator(Selectors.JOB_CARD).all()
        println("    [searchAndApply] 滚动加载结束，共找到 ${jobCards.size} 个职位")
        viewModel.addLog("找到 ${jobCards.size} 个职位")

        println("    [searchAndApply] 开始遍历所有职位...")
        for ((index, card) in jobCards.withIndex()) {
            println("\n--- [searchAndApply] 处理第 ${index + 1} / ${jobCards.size} 个职位 ---")

            println("    [searchAndApply] 检查投递频率限制...")
            if (!checkRateLimit()) {
                viewModel.addLog("已达到投递限制，暂停投递")
                println("    [searchAndApply] 已达到投递上限，停止当前任务")
                return
            }

            try {
                println("    [searchAndApply] 频率正常，调用 applyToJob ...")
                if (applyToJob(card)) {
                    println("    [searchAndApply] 投递成功，更新计数器")
                    updateApplyCount()
                }
            } catch (e: Exception) {
                viewModel.addLog("投递失败: ${e.message}")
                println("    [searchAndApply] applyToJob 抛出异常: ${e.message}")
            }

            val randomDelay = Random.nextLong(rateLimit.minInterval, rateLimit.maxInterval)
            println("    [searchAndApply] 单个职位处理完毕，随机延迟 ${randomDelay}ms...")
            delay(randomDelay)
        }
    }

    private fun buildSearchUrl(keyword: String, city: String): String {
        println("--> [buildSearchUrl] 开始构建搜索URL...")
        val criteria = config.searchCriteria
        val params = mutableListOf<String>()

        val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")
        params.add("query=$encodedKeyword")
        params.add("city=$city")

        viewModel.addLog("当前搜索条件：")
        viewModel.addLog("- 经验要求：${criteria.experience}")
        viewModel.addLog("- 学历要求：${criteria.degree}")
        viewModel.addLog("- 薪资范围：${criteria.salary}")
        viewModel.addLog("- 工作类型：${criteria.jobType}")
        viewModel.addLog("- 公司规模：${criteria.scale}")
        viewModel.addLog("- 融资阶段：${criteria.stage}")

        criteria.experience.takeIf { it.isNotEmpty() }?.let { params.add("experience=$it") }
        criteria.degree.takeIf { it.isNotEmpty() }?.let { params.add("degree=$it") }
        criteria.salary.takeIf { it.isNotEmpty() }?.let { params.add("salary=$it") }
        criteria.jobType.takeIf { it.isNotEmpty() }?.let { params.add("jobType=$it") }
        criteria.scale.takeIf { it.isNotEmpty() }?.let { params.add("scale=$it") }
        criteria.stage.takeIf { it.isNotEmpty() }?.let { params.add("stage=$it") }

        val finalUrl = "$homeUrl/web/geek/job?${params.joinToString("&")}"
        viewModel.addLog("构建的搜索URL: $finalUrl")
        println("<-- [buildSearchUrl] URL构建完成: $finalUrl")
        return finalUrl
    }

    private suspend fun applyToJob(jobCard: Locator): Boolean {
        println("--> [applyToJob] 开始处理单个投递流程...")
        val jobName = jobCard.locator(Selectors.JOB_NAME).textContent()
        val companyName = jobCard.locator(Selectors.COMPANY_NAME).textContent()
        println("    [applyToJob] 职位: $jobName, 公司: $companyName")

        if (!ApplyCheck.canProceed(companyName,jobName)) {
            viewModel.addLog("公司被加入黑名单或岗位已投递过，跳过")
            return false
        }

        println("    [applyToJob] 已打开新标签页用于职位详情")
        val jobPage = context.newPage()
        try {
            val jobUrl = jobCard.locator(Selectors.JOB_NAME).getAttribute("href")
            if (jobUrl == null) {
                println("    [applyToJob] 无法获取职位URL，跳过")
                jobPage.close()
                return false
            }

            jobPage.navigate(homeUrl + jobUrl)
            println("    [applyToJob] 已导航至职位详情页，等待页面加载...")
            jobPage.waitForLoadState(LoadState.NETWORKIDLE)

            println("    [applyToJob] 检查是否满足投递条件 (canApply)...")
            if (!canApply(jobPage)) {
                println("    [applyToJob] 条件不满足，跳过该职位")
                jobPage.close()
                return false
            }

            val jobDescription = jobPage.locator(Selectors.JOB_DESCRIPTION).textContent()
            println("[applyToJob] 条件满足，已获取JD内容(长度: ${jobDescription.length})，准备交由AI分析...")

            var response: String? = null
            for (i in 0..config.retryTimes) {
                if (response == null || response.isEmpty()){
                    val logText = if (i == 0 ) "正在使用AI处理" else "AI处理JD失败，正在重试... ($i/${config.retryTimes})"
                    println("    [applyToJob] 正在调用AI (第 ${i+1} 次尝试)...")
                    withContext(Dispatchers.Main){
                        viewModel.addLog(logText)
                    }
                    val aiResponse = withContext(Dispatchers.IO) {
                        AiManager.oneTimeRequest(
                            provider = AiProvider.GEMINI,
                            message = "这是JD:\n$jobDescription",
                            prompt = AppConfig.getFilledPrompt()
                        )
                    }
                    response = aiResponse.getOrNull()
                    println("    [applyToJob] 收到AI响应: $response")
                }
            }

            if (response == null || response.contains("false")) {
                println("    [applyToJob] AI分析结果为跳过，关闭标签页")
                viewModel.addLog("跳过职位: $companyName - $jobName")
                jobPage.close()
                return false
            }

            if (config.debug) {
                println("    [applyToJob] [调试模式] 模拟投递，不执行实际操作")
                viewModel.addLog("调试模式：模拟投递职位: $companyName - $jobName")
                viewModel.addLog("调试模式：将发送消息: $response")
                jobPage.close()
                return true
            }

            println("[applyToJob] 点击'发送'/'立即沟通'按钮...")
            jobPage.getByRole(
                AriaRole.LINK,
                Page.GetByRoleOptions().apply { name = "立即沟通" }
            ).click()

            delay(rateLimit.pageInterval)

            println("    [applyToJob] 准备在输入框输入AI生成的消息...")
            jobPage.locator(Selectors.CHAT_INPUT).pressSequentially(response.getGreeting(),
                Locator.PressSequentiallyOptions().setDelay(Random.nextDouble(80.0, 200.0)))

            println("    [applyToJob] 点击最终的发送按钮...")
            jobPage.locator(Selectors.SEND_BUTTON).click()

            viewModel.addLog("已投递: $companyName - $jobName")
            submittedCount++
            println("<-- [applyToJob] 投递成功！")
            ApplyCheck.recordSubmission(companyName,jobName)
            delay(rateLimit.pageInterval)
            jobPage.close()
            return true

        } catch (e: Exception) {
            println("<-- [applyToJob] 投递过程中发生异常: ${e.message}")
            jobPage.close()
            throw Exception("投递过程发生错误: ${e.message}")
        }
    }

    override suspend fun submitResume() {
        // Boss直聘不需要实现这个方法
    }

    // 原始的canApply方法（保留用于打开新标签页的方式）
    private suspend fun canApply(page: Page): Boolean {
        println("--> [canApply] 开始检查投递条件...")
        try {
            if (page.locator(Selectors.LIMIT_MESSAGE).count() > 0) {
                println("    [canApply] 检查结果: 已达上限")
                return false
            }
            println("--> [canApply] 上限检查通过...")
            val hrActiveTime = page.locator(Selectors.HR_ACTIVE_TIME)
            println("    [canApply] HR活跃时间: ${hrActiveTime.count()}")
            if (hrActiveTime.count() > 0) {
                if (hrActiveTime.textContent()?.contains("日前活跃") == true) {
                    println("    [canApply] 检查结果: HR不活跃")
                    return false
                }
            }


            println("<-- [canApply] 检查结果: 可以投递")
            return true
        } catch (e: Exception) {
            println("<-- [canApply] 检查时发生异常: ${e.message}")
            return false
        }
    }

    private fun checkRateLimit(): Boolean {
        println("--> [checkRateLimit] 正在检查频率...")
        val currentTime = System.currentTimeMillis()

        if (currentTime - hourStartTime >= 3600000) {
            hourlyApplyCount = 0
            hourStartTime = currentTime
        }

        if (currentTime - lastApplyTime < rateLimit.minInterval) {
            println("<-- [checkRateLimit] 检查结果: false (间隔太短)")
            return false
        }

        if (hourlyApplyCount >= rateLimit.maxPerHour) {
            println("<-- [checkRateLimit] 检查结果: false (每小时超限)")
            return false
        }

        if (dailyApplyCount >= rateLimit.maxPerDay) {
            println("<-- [checkRateLimit] 检查结果: false (每日超限)")
            return false
        }

        println("<-- [checkRateLimit] 检查结果: true (通过)")
        return true
    }

    private fun updateApplyCount() {
        println("--> [updateApplyCount] 更新投递计数...")
        lastApplyTime = System.currentTimeMillis()
        hourlyApplyCount++
        dailyApplyCount++
        println("<-- [updateApplyCount] 计数已更新: hourly=$hourlyApplyCount, daily=$dailyApplyCount")
    }
}