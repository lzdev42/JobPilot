package ai.gemini

/**
 * Gemini API的配置类
 */
data class GeminiConfig(
    val apiKey: String,
    val endpoint: String = "https://generativelanguage.googleapis.com/v1beta",
    val defaultModel: String = "gemini-2.5-flash",
    val timeoutMillis: Long = 30000
)
