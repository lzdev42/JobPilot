package utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 简单的HTTP客户端封装
 */
class HttpRequestor {
    // Ktor客户端实例
    private val client = HttpClient(CIO)

    /**
     * 使用suspend函数发送GET请求（类似await方式）
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Result<String> {
        return try {
            val response = client.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("HTTP错误: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 使用回调方式发送GET请求（类似Swift的逃逸闭包）
     */
    fun getWithCallback(
        url: String,
        headers: Map<String, String> = emptyMap(),
        onLoading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        onLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = get(url, headers)
                if (result.isSuccess) {
                    onSuccess(result.getOrThrow())
                } else {
                    onError(result.exceptionOrNull() as Exception)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    /**
     * 使用suspend函数发送POST请求
     */
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): Result<String> {
        return try {
            val response = client.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("HTTP错误: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 使用回调方式发送POST请求
     */
    fun postWithCallback(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        onLoading: () -> Unit = {},
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        onLoading()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = post(url, body, headers)
                if (result.isSuccess) {
                    onSuccess(result.getOrThrow())
                } else {
                    onError(result.exceptionOrNull() as Exception)
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    companion object {
        // 单例实例
        private var instance: HttpClient? = null

        // 获取单例实例
        fun getInstance(): HttpClient {
            if (instance == null) {
                instance = HttpClient()
            }
            return instance!!
        }
    }
}