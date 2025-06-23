package playwright

data class SearchCriteria(
    val keywords: List<String>,
    val cities: List<String> = emptyList(),  // 改为cities列表
    val experience: String = "",
    val degree: String = "",
    val salary: String = "",
    val jobType: String = "",
    val scale: String = "",
    val stage: String = "",
)

data class PlaywrightConfig(
    val headless: Boolean = false,
    val userAgent: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    val loginTimeout: Int = 300,  // 登录超时时间，单位：秒
    val retryTimes: Int = 3,      // 操作重试次数
    val retryDelay: Long = 5000,  // 重试延迟，单位：毫秒
    val searchCriteria: SearchCriteria, // 搜索条件
    val maxPages: Int = 10,       // 最大搜索页数
    val debug: Boolean = false     // 是否为调试模式
)