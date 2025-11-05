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
import utils.parseJsonResponse
import kotlin.random.Random

class BossJobSeeker(
    viewModel: LogViewModel,
    config: PlaywrightConfig,
    private val shouldStop: () -> Boolean = { false }  // 停止检查函数
) : BaseJobSeeker(viewModel, config) {

    override val loginUrl = "https://www.zhipin.com/web/user/?ka=header-login"
    override val homeUrl = "https://www.zhipin.com"

    // 投递频率控制
    private data class RateLimit(
        val minInterval: Long = 3000, // 最小间隔3秒
        val maxInterval: Long = 10000, // 最大间隔10秒
        val pageInterval: Long = Random.nextLong(3000, 10000),  // 随机3到10秒
        val maxPerHour: Int = 50,       // 每小时最大投递数
        val maxPerDay: Int = AppConfig.bossApplyLimit  // 每天最大投递数，从配置读取
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
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始检查登录状态...")
        }
        val result = try {
            exists(Selectors.LOGIN_SUCCESS_HEADER)
        } catch (e: Exception) {
            false
        }
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 登录状态检查完成: $result")
        }
        return result
    }

    /**
     * 获取登录页面的二维码URL
     * @return 完整的二维码图片URL，如果找不到二维码则返回null
     */
    suspend fun getQrCodeUrl(): String? {
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始获取二维码URL...")
        }
        try {
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 等待二维码图片元素出现...")
            }
            waitForSelector(Selectors.QR_CODE_IMAGE)

            val relativePath = page.locator(Selectors.QR_CODE_IMAGE).getAttribute("src")
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 获取到二维码src属性: $relativePath")
            }
            if (relativePath != null) {
                val fullQrCodeUrl = "https://www.zhipin.com" + relativePath
                viewModel.addLog("获取到二维码URL: $fullQrCodeUrl")
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 成功构建二维码URL")
                }
                return fullQrCodeUrl
            } else {
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 未能获取到二维码src属性")
                }
            }
        } catch (e: Exception) {
            viewModel.addLog("获取二维码URL失败: ${e.message}")
        }
        return null
    }

    override suspend fun waitForLogin() {
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始等待用户登录...")
        }
        navigate(loginUrl)
        viewModel.addLog("请在打开的浏览器中完成登录")

        try {
            delay(2000)
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 检查是否存在APP扫码登录切换按钮...")
            }
            if (exists(Selectors.LOGIN_SCAN_SWITCH)) {
                viewModel.addLog("正在切换到APP扫码登录模式...")
                click(Selectors.LOGIN_SCAN_SWITCH)
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 已点击APP扫码登录按钮")
                }
                viewModel.addLog("已切换到APP扫码登录模式，请使用手机APP扫描二维码登录")

                val qrCodeUrl = getQrCodeUrl()
                if (qrCodeUrl != null) {
                    viewModel.addLog("二维码URL: $qrCodeUrl")
                }
            }
        } catch (e: Exception) {
            viewModel.addLog("切换到扫码登录模式失败: ${e.message}，请手动切换到扫码登录")
        }

        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 进入登录状态检查循环...")
        }
        var waitTime = 0
        while (!isLoggedIn()) {
            delay(1000)
            waitTime++
            if (AppConfig.isDebugMode && waitTime % 10 == 0) {
                viewModel.addLog("[DEBUG] 等待登录中... (${waitTime}s / ${config.loginTimeout}s)")
            }
            if (waitTime >= config.loginTimeout) {
                viewModel.addLog("登录超时！")
                throw Exception("登录超时，请重试")
            }
        }
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 检测到登录成功！")
        }
        viewModel.addLog("登录成功，开始执行任务")
    }

    override suspend fun search(keywords: List<String>) {
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始执行总的搜索任务...")
        }
        for (city in config.searchCriteria.cities) {
            if (shouldStop()) {
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 检测到停止信号，退出城市循环")
                }
                viewModel.addLog("任务已被停止")
                return
            }
            
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 开始处理城市: $city")
            }
            for (keyword in keywords) {
                if (shouldStop()) {
                    if (AppConfig.isDebugMode) {
                        viewModel.addLog("[DEBUG] 检测到停止信号，退出关键词循环")
                    }
                    viewModel.addLog("任务已被停止")
                    return
                }
                
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 开始处理关键词: '$keyword'")
                }
                try {
                    if (AppConfig.isDebugMode) {
                        viewModel.addLog("[DEBUG] 即将调用searchAndApply...")
                    }
                    searchAndApply(keyword, city)
                } catch (e: Exception) {
                    viewModel.addLog("搜索关键词 $keyword 在城市 $city 时发生错误: ${e.message}")
                    if (AppConfig.isDebugMode) {
                        viewModel.addLog("[DEBUG] 搜索中断: 关键词'$keyword'在城市'$city'的搜索发生错误: ${e.message}")
                    }
                    delay(rateLimit.pageInterval)
                }
            }
        }
    }

    private suspend fun searchAndApply(keyword: String, city: String) {
        viewModel.addLog("开始搜索: $keyword (城市: $city)")

        val searchUrl = buildSearchUrl(keyword, city)
        navigate(searchUrl)
        viewModel.addLog("已导航至搜索页: $searchUrl")
        println("--> [searchAndApply] 已导航至搜索页，等待列表加载...")
        
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 已导航至搜索页，等待列表加载...")
        }
        
        // 等待职位列表容器出现
        println("    [searchAndApply] 等待职位列表容器出现 (选择器: ${Selectors.JOB_LIST_CONTAINER})...")
        try {
            waitForSelector(Selectors.JOB_LIST_CONTAINER)
            println("    [searchAndApply] ✓ 职位列表容器已出现")
            viewModel.addLog("职位列表容器已加载")
        } catch (e: Exception) {
            println("    [searchAndApply] ✗ 等待职位列表容器超时: ${e.message}")
            viewModel.addLog("错误：无法找到职位列表容器 - ${e.message}")
            throw e
        }

        // 检查初始职位数量
        val initialCount = page.locator(Selectors.JOB_CARD).count()
        println("    [searchAndApply] 初始职位数量: $initialCount")
        viewModel.addLog("初始职位数量: $initialCount")
        
        if (initialCount == 0) {
            println("    [searchAndApply] ⚠️ 警告：初始职位数量为0，可能选择器不正确或页面结构已变化")
            viewModel.addLog("警告：未找到任何职位，请检查选择器是否正确")
            // 尝试打印页面相关信息以便调试
            try {
                val pageTitle = page.title()
                val pageUrl = page.url()
                println("    [searchAndApply] 当前页面标题: $pageTitle")
                println("    [searchAndApply] 当前页面URL: $pageUrl")
                // 尝试查找其他可能的列表容器
                val alternativeSelectors = listOf(
                    "div.job-list",
                    "ul.job-list",
                    "div[class*='job']",
                    "li[class*='job']"
                )
                for (altSelector in alternativeSelectors) {
                    val count = page.locator(altSelector).count()
                    if (count > 0) {
                        println("    [searchAndApply] 发现可能的替代选择器 '$altSelector' 找到 $count 个元素")
                    }
                }
            } catch (e: Exception) {
                println("    [searchAndApply] 获取页面信息失败: ${e.message}")
            }
        }

        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始滚动页面以加载更多职位...")
        }
        var previousCount = initialCount
        var unchangedCount = 0
        while (unchangedCount < 2) {
            // 先滚动到底部
            println("    [searchAndApply] 执行滚动到底部...")
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            delay(2000)  // 等待新内容加载

            // 然后检查职位数量是否增加
            val currentCount = page.locator(Selectors.JOB_CARD).count()
            println("    [searchAndApply] 滚动后，当前职位数: $currentCount (之前: $previousCount)")
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 滚动后，当前职位数: $currentCount")
            }

            if (currentCount > previousCount) {
                // 职位数量增加了，重置计数器并更新记录
                previousCount = currentCount
                unchangedCount = 0
                println("    [searchAndApply] 职位数量增加，继续滚动...")
            } else {
                // 职位数量没有增加
                unchangedCount++
                println("    [searchAndApply] 职位数量未变化 ($unchangedCount/2)")
            }
        }

        val jobCards = page.locator(Selectors.JOB_CARD).all()
        println("    [searchAndApply] 滚动加载结束，共找到 ${jobCards.size} 个职位卡片")
        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 滚动加载结束，共找到 ${jobCards.size} 个职位")
        }
        viewModel.addLog("找到 ${jobCards.size} 个职位")
        
        // 打印前几个职位的信息以便调试
        if (jobCards.isNotEmpty()) {
            println("    [searchAndApply] 开始打印前3个职位的信息...")
            for ((idx, card) in jobCards.take(3).withIndex()) {
                try {
                    val jobName = card.locator(Selectors.JOB_NAME).textContent() ?: "未知"
                    val companyName = card.locator(Selectors.COMPANY_NAME).textContent() ?: "未知"
                    println("    [searchAndApply] 职位 ${idx + 1}: $jobName @ $companyName")
                } catch (e: Exception) {
                    println("    [searchAndApply] 获取职位 ${idx + 1} 信息失败: ${e.message}")
                }
            }
        } else {
            println("    [searchAndApply] ⚠️ 警告：jobCards 列表为空，无法获取职位信息")
            viewModel.addLog("警告：职位列表为空，请检查页面是否正常加载")
        }

        if (AppConfig.isDebugMode) {
            viewModel.addLog("[DEBUG] 开始遍历所有职位...")
        }
        for ((index, card) in jobCards.withIndex()) {
            if (shouldStop()) {
                if (AppConfig.isDebugMode) {
                    viewModel.addLog("[DEBUG] 检测到停止信号，退出职位遍历循环")
                }
                viewModel.addLog("任务已被停止")
                return
            }
            
            if (AppConfig.isDebugMode) {
                viewModel.addLog("[DEBUG] 处理第 ${index + 1}/${jobCards.size} 个职位")
            }

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
                        val finalPrompt = AppConfig.getFilledPrompt().replace("PLACEHOLDER_FOR_JD", jobDescription)
                        
                        // Debug模式下打印最终提示词和JD信息
                        if (AppConfig.isDebugMode) {
                            println("=== AI DEBUG: BossJobSeeker 调用AI ===")
                            println("公司: $companyName")
                            println("职位: $jobName") 
                            println("JD长度: ${jobDescription.length} 字符")
                            println("最终提示词:")
                            println(finalPrompt)
                            println("=== AI DEBUG: BossJobSeeker 结束 ===")
                        }
                        
                        AiManager.oneTimeRequest(
                            provider = AiProvider.GEMINI,
                            message = "",  // 不再通过message传递JD
                            prompt = finalPrompt
                        )
                    }
                    response = aiResponse.getOrNull()
                    println("    [applyToJob] 收到AI响应: $response")
                }
            }

            if (response == null) {
                println("    [applyToJob] AI响应为null，跳过该职位")
                viewModel.addLog("跳过职位: $companyName - $jobName (AI响应为空)")
                jobPage.close()
                return false
            }

            val (matchStatus, reasoning, greetingMessage) = response.parseJsonResponse()
            if (!matchStatus) {
                println("    [applyToJob] AI分析结果为跳过，关闭标签页")
                viewModel.addLog("跳过职位: $companyName - $jobName (原因: $reasoning)")
                jobPage.close()
                return false
            }

            if (config.debug) {
                println("    [applyToJob] [调试模式] 模拟投递，不执行实际操作")
                viewModel.addLog("调试模式：模拟投递职位: $companyName - $jobName")
                viewModel.addLog("调试模式：匹配推理: $reasoning")
                viewModel.addLog("调试模式：将发送消息: $greetingMessage")
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
            jobPage.locator(Selectors.CHAT_INPUT).pressSequentially(greetingMessage,
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