package ai.gemini

import ai.gemini.model.ChatMessage

/**
 * Gemini聊天会话类，维护对话历史
 */
class GeminiChatSession(
    private val client: GeminiClient,
    initialPrompt: String? = null
) {
    private val history = mutableListOf<ChatMessage>()
    
    init {
        // 如果提供了初始提示词，添加到历史
        initialPrompt?.let {
            history.add(ChatMessage(role = "user", content = it))
        }
    }
    
    /**
     * 发送消息并获取响应
     */
    suspend fun sendMessage(message: String): Result<String> {
        // 添加用户消息到历史
        history.add(ChatMessage(role = "user", content = message))
        
        // 发送包含完整历史的请求
        val response = client.generateTextWithHistory(history)
        
        // 如果成功，将AI响应添加到历史
        if (response.isSuccess) {
            history.add(ChatMessage(role = "model", content = response.getOrThrow()))
        }
        
        return response
    }
    
    /**
     * 使用回调方式发送消息
     */
    fun sendMessageWithCallback(
        message: String,
        onLoading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // 添加用户消息到历史
        history.add(ChatMessage(role = "user", content = message))
        
        // 发送包含完整历史的请求
        client.generateTextWithHistoryCallback(
            history = history,
            onLoading = onLoading,
            onSuccess = { response ->
                // 将AI响应添加到历史
                history.add(ChatMessage(role = "model", content = response))
                onSuccess(response)
            },
            onError = onError
        )
    }
    
    /**
     * 清除历史，重置会话
     */
    fun clearHistory() {
        history.clear()
    }
    
    /**
     * 获取当前会话历史
     */
    fun getHistory(): List<ChatMessage> {
        return history.toList()
    }
}
