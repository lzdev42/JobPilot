package ai.gemini

import ai.gemini.model.ChatMessage
import kotlinx.serialization.json.*
import utils.HttpRequestor

/**
 * Gemini API客户端
 */
class GeminiClient(val config: GeminiConfig) {
    private val httpRequestor = HttpRequestor()
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 生成文本（单次请求模式）
     */
    suspend fun generateText(prompt: String): Result<String> {
        return try {
            val url = "${config.endpoint}/models/${config.defaultModel}:generateContent?key=${config.apiKey}"
            
            val requestBody = buildRequestJson(prompt)
            
            val result = httpRequestor.post(url, requestBody)
            
            if (result.isSuccess) {
                parseResponse(result.getOrThrow())
            } else {
                Result.failure(GeminiException("请求失败: ${result.exceptionOrNull()?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(GeminiException("生成文本失败", e))
        }
    }

    /**
     * 使用回调方式生成文本
     */
    fun generateTextCallback(
        prompt: String,
        onLoading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val url = "${config.endpoint}/models/${config.defaultModel}:generateContent?key=${config.apiKey}"
        val requestBody = buildRequestJson(prompt)
        
        httpRequestor.postWithCallback(
            url = url,
            body = requestBody,
            onLoading = onLoading,
            onSuccess = { response ->
                try {
                    val result = parseResponse(response)
                    if (result.isSuccess) {
                        onSuccess(result.getOrThrow())
                    } else {
                        onError(result.exceptionOrNull() as Exception)
                    }
                } catch (e: Exception) {
                    onError(GeminiException("解析响应失败", e))
                }
            },
            onError = { exception ->
                onError(GeminiException("请求失败", exception))
            }
        )
    }
    
    /**
     * 使用历史记录生成文本（内部方法，由GeminiChatSession调用）
     */
    internal suspend fun generateTextWithHistory(history: List<ChatMessage>): Result<String> {
        return try {
            val url = "${config.endpoint}/models/${config.defaultModel}:generateContent?key=${config.apiKey}"
            
            val requestBody = buildRequestJsonWithHistory(history)
            
            val result = httpRequestor.post(url, requestBody)
            
            if (result.isSuccess) {
                parseResponse(result.getOrThrow())
            } else {
                Result.failure(GeminiException("请求失败: ${result.exceptionOrNull()?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(GeminiException("生成文本失败", e))
        }
    }
    
    /**
     * 使用回调方式和历史记录生成文本（内部方法，由GeminiChatSession调用）
     */
    internal fun generateTextWithHistoryCallback(
        history: List<ChatMessage>,
        onLoading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val url = "${config.endpoint}/models/${config.defaultModel}:generateContent?key=${config.apiKey}"
        val requestBody = buildRequestJsonWithHistory(history)
        
        httpRequestor.postWithCallback(
            url = url,
            body = requestBody,
            onLoading = onLoading,
            onSuccess = { response ->
                try {
                    val result = parseResponse(response)
                    if (result.isSuccess) {
                        onSuccess(result.getOrThrow())
                    } else {
                        onError(result.exceptionOrNull() as Exception)
                    }
                } catch (e: Exception) {
                    onError(GeminiException("解析响应失败", e))
                }
            },
            onError = { exception ->
                onError(GeminiException("请求失败", exception))
            }
        )
    }
    
    /**
     * 创建聊天会话
     */
    fun createChatSession(initialPrompt: String? = null): GeminiChatSession {
        return GeminiChatSession(this, initialPrompt)
    }
    
    /**
     * 构建单个提示的请求JSON
     */
    private fun buildRequestJson(prompt: String): String {
        val jsonObject = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
        }
        
        return jsonObject.toString()
    }
    
    /**
     * 构建带历史记录的请求JSON
     */
    private fun buildRequestJsonWithHistory(history: List<ChatMessage>): String {
        val jsonObject = buildJsonObject {
            putJsonArray("contents") {
                history.forEach { message ->
                    addJsonObject {
                        put("role", message.role)
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", message.content)
                            }
                        }
                    }
                }
            }
        }
        
        return jsonObject.toString()
    }
    
    /**
     * 解析API响应
     */
    private fun parseResponse(responseJson: String): Result<String> {
        return try {
            val jsonElement = json.parseToJsonElement(responseJson)
            val candidates = jsonElement.jsonObject["candidates"]?.jsonArray
            
            if (candidates != null && candidates.isNotEmpty()) {
                val content = candidates[0].jsonObject["content"]?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                
                if (parts != null && parts.isNotEmpty()) {
                    val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                    
                    if (text != null) {
                        Result.success(text)
                    } else {
                        Result.failure(GeminiException("响应中没有文本内容"))
                    }
                } else {
                    Result.failure(GeminiException("响应中没有parts字段"))
                }
            } else {
                // 检查是否有错误信息
                val error = jsonElement.jsonObject["error"]?.jsonObject
                val message = error?.get("message")?.jsonPrimitive?.content
                
                if (message != null) {
                    Result.failure(GeminiException("API错误: $message"))
                } else {
                    Result.failure(GeminiException("响应中没有candidates字段"))
                }
            }
        } catch (e: Exception) {
            Result.failure(GeminiException("解析响应失败", e))
        }
    }
}
