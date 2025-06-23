package playwright

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import logview.LogViewModel
import java.net.URLEncoder
import java.util.concurrent.TimeoutException

/**
 * 前程无忧(51job)自动投递简历的现代化、Playwright-Native 实现。
 *
 * 设计哲学:
 * 1. 流程清晰: 将复杂的投递流程拆分为职责单一的小方法。
 * 2. 拥抱Playwright: 充分利用 Playwright 的 Locator API 和自动等待机制。
 * 3. 健壮性优先: 内置优雅的重试和异常处理逻辑。
 * 4. Kotlin风格: 使用协程和现代Kotlin语法编写简洁、可读的代码。
 */
class Job51JobSeeker(
    viewModel: LogViewModel,
    config: PlaywrightConfig
) : BaseJobSeeker(viewModel, config) {

    override val loginUrl = "https://login.51job.com/login.php"
    override val homeUrl = "https://www.51job.com"
    private val searchUrl = "https://we.51job.com/pc/search"

    // 将速率限制集中管理，并提供更语义化的名称
    private object RateLimits {
        const val BATCH_APPLY_DELAY = 10_000L // 批量投递前的固定延迟，应对51job的限制
        const val PAGE_NAVIGATION_DELAY = 3_000L  // 翻页后的等待
        const val POPUP_CHECK_DELAY = 2_000L    // 操作后检查弹窗的延迟
    }

    private object Selectors {
        // 登录相关
        const val LOGIN_SUCCESS_INDICATOR = "a.uname:not(:text('登录'))" // 更精确的登录成功选择器

        // 列表页核心元素
        const val JOB_LIST_CONTAINER = "div.joblist"
        const val JOB_CARD_CHECKBOX = "div.ick"
        const val JOB_CARD_TITLE = "p.jname"
        const val JOB_CARD_COMPANY = "p.cname"
        const val BATCH_APPLY_BUTTON = "div.tabs_in >> text=投递选中职位" // 使用Playwright文本选择器
        const val PAGE_INPUT = "#jump_page"
        const val PAGE_JUMP_BUTTON = "span.jumpPage"

        // 弹窗与提示
        const val SUCCESS_POPUP = "div.successContent"
        const val SUCCESS_POPUP_CLOSE_BUTTON = "i.van-icon-cross"
        const val EXTERNAL_APPLY_POPUP = "div.el-dialog__body >> text=需要到企业招聘平台单独申请"
        const val EXTERNAL_APPLY_POPUP_CLOSE_BUTTON = "button.el-dialog__headerbtn"
        const val VERIFICATION_POPUP_TITLE = "p.waf-nc-title:text('安全验证')"
    }

    // --- 核心业务逻辑实现 ---

    override suspend fun isLoggedIn(): Boolean {
        // 使用更健壮的方式检查登录状态，等待元素出现，避免瞬时网络问题导致误判
        return try {
            page.waitForSelector(Selectors.LOGIN_SUCCESS_INDICATOR, Page.WaitForSelectorOptions().setTimeout(5000.0))
            true
        } catch (e: TimeoutException) {
            false
        }
    }

    override suspend fun search(keywords: List<String>) {
        if (!isLoggedIn()) {
            waitForLogin()
        }
        for (keyword in keywords) {
            viewModel.addLog("开始处理关键词: $keyword")
            try {
                processKeyword(keyword)
            } catch (e: Exception) {
                viewModel.addLog("处理关键词 '$keyword' 时发生严重错误: ${e.message}")
                handleCriticalError(e) // 出现严重错误（如验证码）时，可以决定是否中断
            }
        }
    }

    /**
     * 处理单个关键词的完整流程
     */
    private suspend fun processKeyword(keyword: String) {
        navigateToFirstPage(keyword)

        // 51job的特殊限制：在进行任何批量操作前，先等待一段时间
        viewModel.addLog("遵循51job策略，在批量投递前等待 ${RateLimits.BATCH_APPLY_DELAY / 1000} 秒...")
        delay(RateLimits.BATCH_APPLY_DELAY)

        // 从第一页开始循环处理
        for (currentPage in 1..config.maxPages) {
            if (currentPage > 1) {
                navigateToPage(currentPage)
            }

            val jobsApplied = applyToJobsOnCurrentPage()
            if (jobsApplied == 0) {
                viewModel.addLog("第 $currentPage 页没有新的投递，可能已是最后一页或无合适职位。")
                // 可以增加逻辑，如果连续2页没有投递，则结束该关键词
                break
            }

            submittedCount += jobsApplied
            viewModel.addLog("第 $currentPage 页完成投递，累计投递 $submittedCount 个职位。")
        }
    }

    /**
     * 导航到关键词的第一页
     */
    private suspend fun navigateToFirstPage(keyword: String) {
        val url = buildSearchUrl(keyword)
        viewModel.addLog("导航至搜索页: $url")
        navigate(url)
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED)
        page.waitForSelector(Selectors.JOB_LIST_CONTAINER) // 确保列表容器已加载
    }

    /**
     * 构建搜索URL
     */
    private fun buildSearchUrl(keyword: String): String {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        // 使用更Kotlin风格的构建方式
        return buildString {
            append(searchUrl)
            append("?keyword=$encodedKeyword")
            config.searchCriteria.cities.takeIf { it.isNotEmpty() }?.let { append("&jobArea=${it.joinToString(",")}") }
            //config.searchCriteria.salary.takeIf { it.isNotEmpty() }?.let { append("&salary=${it.joinToString(",")}") }
        }
    }

    /**
     * 跳转到指定页码
     */
    private suspend fun navigateToPage(pageNum: Int) {
        viewModel.addLog("准备跳转到第 $pageNum 页...")
        try {
            safeFill(Selectors.PAGE_INPUT, pageNum.toString())
            safeClick(Selectors.PAGE_JUMP_BUTTON)
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED)
            page.waitForSelector(Selectors.JOB_LIST_CONTAINER)
            delay(RateLimits.PAGE_NAVIGATION_DELAY) // 翻页后短暂等待，模拟人类行为
        } catch (e: Exception) {
            throw IllegalStateException("跳转到第 $pageNum 页失败，可能是最后一页或页面结构已改变。", e)
        }
    }

    /**
     * 处理当前页面的所有职位投递，返回成功投递的数量
     */
    private suspend fun applyToJobsOnCurrentPage(): Int {
        val checkboxes = page.locator(Selectors.JOB_CARD_CHECKBOX).all()
        if (checkboxes.isEmpty()) {
            viewModel.addLog("当前页面未找到任何职位。")
            return 0
        }

        viewModel.addLog("在当前页面找到 ${checkboxes.size} 个职位，开始筛选和勾选...")

        // 统一勾选，Playwright的Locator是动态的，不需要像Selenium那样担心元素过时
        for (checkbox in checkboxes) {
            // 这里可以加入黑名单公司的过滤逻辑
            checkbox.click()
        }
        viewModel.addLog("已勾选所有 ${checkboxes.size} 个职位。")

        // 点击批量投递按钮
        try {
            safeClick(Selectors.BATCH_APPLY_BUTTON)
            viewModel.addLog("已点击'投递选中职位'按钮。")
        } catch (e: Exception) {
            viewModel.addLog("无法点击批量投递按钮，可能当前页已全部投递过。跳过本页。")
            return 0 // 如果无法点击投递，说明没有可投递的，返回0
        }

        // 统一处理所有可能的弹窗
        handlePostApplyPopups()

        return checkboxes.size // 假设勾选的都成功发起了投递
    }

    /**
     * 在投递操作后，统一处理可能出现的各种弹窗
     */
    private suspend fun handlePostApplyPopups() {
        delay(RateLimits.POPUP_CHECK_DELAY) // 等待弹窗出现

        // 使用 try-with-timeout 结构，优雅地处理弹窗不存在的情况
        tryTo(timeout = 3000.0) {
            val successPopup = page.locator(Selectors.SUCCESS_POPUP)
            if (successPopup.isVisible()) {
                viewModel.addLog("检测到投递成功弹窗。")
                successPopup.locator(Selectors.SUCCESS_POPUP_CLOSE_BUTTON).click()
                viewModel.addLog("已关闭成功弹窗。")
            }
        }

        tryTo(timeout = 3000.0) {
            val externalPopup = page.locator(Selectors.EXTERNAL_APPLY_POPUP)
            if (externalPopup.isVisible()) {
                viewModel.addLog("检测到需要外部申请的弹窗。")
                page.locator(Selectors.EXTERNAL_APPLY_POPUP_CLOSE_BUTTON).click()
                viewModel.addLog("已关闭外部申请弹窗。")
            }
        }
    }

    /**
     * 出现严重错误（如验证码）时的处理
     */
    private suspend fun handleCriticalError(e: Exception) {
        val isVerification = try {
            page.isVisible(Selectors.VERIFICATION_POPUP_TITLE)
        } catch (ex: Exception) {
            false
        }

        if (isVerification) {
            viewModel.addLog("[严重错误] 检测到人机验证！程序将停止当前任务。")
            // 这里可以触发一个全局停止的信号
            throw IllegalStateException("人机验证出现，无法继续。", e)
        }
    }

    // --- 辅助方法 ---

    // 这是一个辅助函数，用于优雅地处理可能不存在的元素操作
    private suspend fun tryTo(timeout: Double = 1000.0, action: suspend () -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                // Playwright的多数操作自带超时，这里为整个代码块设置一个外部超时
                kotlinx.coroutines.withTimeout(timeout.toLong()) {
                    action()
                }
            }
        } catch (e: Exception) {
            // 忽略超时或元素未找到的异常
        }
    }

    override suspend fun submitResume() {
        // 在此实现中，简历投递在search流程中完成，此方法可留空。
        viewModel.addLog("所有关键词处理完毕。")
    }
}