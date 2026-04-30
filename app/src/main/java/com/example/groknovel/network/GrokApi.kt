package com.example.groknovel.network

import com.example.groknovel.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Grok API 网络请求类
 * 使用 OkHttp 实现与 Grok 4.20 API 的通信
 */
class GrokApi {

    companion object {
        private const val BASE_URL = "https://api.apiyi.com/v1/chat/completions"
        private const val MODEL = "grok-4.20-beta"
        private const val TEMPERATURE = 0.9
        private const val MAX_TOKENS = 2048
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    // OkHttp 客户端单例
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 生成故事
     * @param prompt 提示词
     * @param storyHistory 之前的故事历史（用于继续创作）
     * @return 生成的故事内容
     * @throws Exception 网络或API错误
     */
    suspend fun generateStory(prompt: String, storyHistory: String = ""): Result<GrokResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 构建请求体
                val requestBody = buildRequestBody(prompt, storyHistory)

                // 构建请求
                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer ${BuildConfig.API_KEY}")
                    .post(requestBody)
                    .build()

                // 发送请求
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val grokResponse = parseResponse(responseBody)
                        Result.success(grokResponse)
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val errorMessage = when (response.code) {
                        401 -> "API Key 无效或已过期"
                        429 -> "请求过于频繁，请稍后重试"
                        500 -> "服务器内部错误"
                        else -> "API 请求失败: ${response.code}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络错误: ${e.message}"))
            }
        }
    }

    /**
     * 构建请求体 JSON
     */
    private fun buildRequestBody(prompt: String, storyHistory: String): RequestBody {
        val messages = JSONArray()

        // 添加系统提示
        val systemMessage = JSONObject().apply {
            put("role", "system")
            put("content", getSystemPrompt())
        }
        messages.put(systemMessage)

        // 如果有历史，添加用户故事背景
        if (storyHistory.isNotEmpty()) {
            val historyMessage = JSONObject().apply {
                put("role", "user")
                put("content", "当前故事背景：\n$storyHistory\n\n请继续这个故事的发展。")
            }
            messages.put(historyMessage)
        }

        // 添加当前提示
        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        messages.put(userMessage)

        // 构建完整请求体并转换为 RequestBody
        val jsonBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", messages)
            put("temperature", TEMPERATURE)
            put("max_tokens", MAX_TOKENS)
        }.toString()
        
        return jsonBody.toRequestBody(JSON_MEDIA_TYPE)
    }

    /**
     * 解析 API 响应
     */
    private fun parseResponse(responseBody: String): GrokResponse {
        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")

        if (choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")
            val finishReason = firstChoice.optString("finish_reason", "stop")

            // 尝试解析故事选项
            val choicesList = parseStoryChoices(content)

            return GrokResponse(
                content = content,
                choices = choicesList,
                finishReason = finishReason
            )
        }

        throw Exception("Invalid API response: no choices found")
    }

    /**
     * 解析故事选项
     * 从生成内容中提取剧情选择项
     */
    private fun parseStoryChoices(content: String): List<String> {
        val choices = mutableListOf<String>()

        // 尝试匹配不同的选项格式
        val patterns = listOf(
            Regex("""(?:选项?|choice|请选择)[:：]\s*\n((?:[A-Da-d][.、:：].+\n?)+)"""),
            Regex("""(?:接下来|你会)[：:]?\s*\n((?:1[.、:：].+\n?)+)"""),
            Regex("""(?:A|B|C|D)[.、:：]\s*(.+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                val optionsText = match.groupValues[1]
                val optionMatches = Regex("""[A-Da-d][.、:：]\s*(.+)""").findAll(optionsText)
                optionMatches.forEach { choices.add(it.groupValues[1].trim()) }
                break
            }
        }

        return choices.distinct().take(4) // 最多返回4个选项
    }

    /**
     * 获取系统提示词
     */
    private fun getSystemPrompt(): String {
        return """
你是一位专业的小说作家，擅长创作引人入胜的故事情节。

创作要求：
1. 使用生动、形象的文字描绘场景和人物
2. 故事要有悬念和转折，吸引读者继续阅读
3. 在故事结尾提供2-4个明确的剧情选择项，让读者决定故事走向
4. 选择项应该多样化，包括冒险、理性、情感等不同方向
5. 保持故事的连贯性和逻辑性

格式要求：
- 故事正文用自然段落描述
- 在结尾用"请选择："标记选项
- 每个选项用字母（A、B、C、D）开头，后面跟具体行动描述
- 选项应该简洁明了，让读者清楚知道选择的后果

示例格式：
[故事内容...]
夜色渐深，你站在分岔路口...

请选择：
A. 继续前进，探索未知的黑暗
B. 回头寻找同伴的帮助
C. 原地休息，等待黎明
D. 仔细观察周围环境，寻找线索
        """.trimIndent()
    }
}

/**
 * Grok API 响应数据类
 */
data class GrokResponse(
    val content: String,
    val choices: List<String>,
    val finishReason: String
)
