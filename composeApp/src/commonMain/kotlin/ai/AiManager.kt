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
        if (provider == AiProvider.GEMINI && geminiConfig != null) {
            val client = GeminiClient(geminiConfig!!)
            return client.createChatSession(prompt).sendMessage(message)
        }else{
            return Result.failure(Exception("AI服务未初始化"))
        }
    }

}