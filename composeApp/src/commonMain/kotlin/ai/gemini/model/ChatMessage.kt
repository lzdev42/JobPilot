package ai.gemini.model

/**
 * 表示聊天消息的数据类
 */
data class ChatMessage(
    val role: String, // "user" 或 "model"
    val content: String
)
