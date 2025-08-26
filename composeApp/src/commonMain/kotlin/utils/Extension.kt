package utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/**
 * AI响应数据结构
 */
@Serializable
data class AiResponse(
    val match_status: Boolean,
    val reasoning: String,
    val greeting_message: String
)

/**
 * 解析AI返回的JSON响应
 * @return Triple(match_status, reasoning, greeting_message)
 */
fun String.parseJsonResponse(): Triple<Boolean, String, String> {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val response = json.decodeFromString<AiResponse>(this.trim())
        Triple(response.match_status, response.reasoning, response.greeting_message)
    } catch (e: SerializationException) {
        // JSON解析错误，暂时保留println用于错误追踪
        println("[ERROR] JSON解析失败: ${e.message}")
        println("[ERROR] 原始响应: $this")
        // 返回默认值，跳过该职位
        Triple(false, "JSON解析失败: ${e.message}", "")
    } catch (e: Exception) {
        // 响应解析异常，暂时保留println用于错误追踪
        println("[ERROR] 解析响应时发生异常: ${e.message}")
        println("[ERROR] 原始响应: $this")
        Triple(false, "响应解析异常: ${e.message}", "")
    }
}

/**
 * 提取招呼语的扩展函数
 * @return 招呼语内容，如果解析失败返回空字符串
 */
fun String.getGreeting(): String {
    val (_, _, greetingMessage) = this.parseJsonResponse()
    return greetingMessage
}