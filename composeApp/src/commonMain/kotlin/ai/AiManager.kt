package ai

import ai.gemini.GeminiClient
import ai.gemini.GeminiConfig
import utils.AppConfig

enum class AiProvider {
    GEMINI,OPENAI
}

object AiManager {
    private val geminiKey = ""
    private var geminiConfig: GeminiConfig? = null
    fun initGemini(){
        geminiConfig = GeminiConfig(AppConfig.geminiAppKey)
    }

    suspend fun oneTimeRequest(provider: AiProvider = AiProvider.GEMINI,
                               message: String,
                               prompt: String): Result<String> {
        // Debug模式下打印AI请求参数
        if (utils.AppConfig.isDebugMode) {
            // 注意：这里需要viewModel参数，暂时保留println用于AI调试
            println("=== AI DEBUG: AiManager.oneTimeRequest 调用参数 ===")
            println("Provider: $provider")
            println("Message: $message")
            println("Prompt: $prompt")
            println("=== AI DEBUG: 参数结束 ===")
        }
        
        if (provider == AiProvider.GEMINI && geminiConfig != null) {
            val client = GeminiClient(geminiConfig!!)
            return client.createChatSession(prompt).sendMessage(message)
        }else{
            return Result.failure(Exception("AI服务未初始化"))
        }
    }

}