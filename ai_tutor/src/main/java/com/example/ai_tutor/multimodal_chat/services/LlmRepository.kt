package com.example.ai_tutor.multimodal_chat.services

import com.example.common.config.AppConstants
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.RetrofitClient
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.common.network.llm.ChatMessage as Message

@Singleton
class LlmRepository
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) {
        suspend fun sendMessage(
            apiKey: String,
            baseUrl: String = AppConstants.BASE_URL,
            modelName: String = AppConstants.DEFAULT_MODEL_NAME,
            prompt: String,
            history: List<Message>,
            imageUrl: String? = null,
        ): Flow<String> =
            flow {
                val api = RetrofitClient.create(apiKey, baseUrl).create(OpenAiService::class.java)

                val messages = mutableListOf<Message>()

                val hasSystemPrompt = history.any { it.role == "system" }
                if (!hasSystemPrompt) {
                    messages.add(Message("system", AppConstants.AI_TUTOR_SYSTEM_PROMPT))
                }

                // 拼接历史记录和当前用户消息
                messages.addAll(history)

                val userContent: Any =
                    if (imageUrl != null) {
                        listOf(
                            com.example.common.network.llm.ContentItem(
                                type = "image_url",
                                imageUrl = com.example.common.network.llm.ImageUrl(url = imageUrl),
                            ),
                            com.example.common.network.llm.ContentItem(type = "text", text = prompt),
                        )
                    } else {
                        prompt
                    }

                messages.add(Message("user", userContent))

                val request =
                    ChatRequest(
                        model = if (imageUrl != null && modelName.startsWith("qwen-turbo")) "qwen-vl-max" else modelName,
                        messages = messages,
                        stream = true,
                    )

                try {
                    val responseBody = api.chatStream(request)
                    responseBody.byteStream().bufferedReader().use { reader ->
                        var line: String?
                        val gson = com.google.gson.Gson()
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.startsWith("data: ")) {
                                val data = currentLine.substring(6)
                                if (data == "[DONE]") break
                                try {
                                    val chunk = gson.fromJson(data, com.example.common.network.llm.ChatResponse::class.java)
                                    val deltaContent =
                                        chunk.choices?.firstOrNull()?.delta?.content
                                            ?: chunk.output?.choices?.firstOrNull()?.message?.content
                                            ?: chunk.output?.text

                                    if (deltaContent != null) {
                                        val deltaText = if (deltaContent is String) deltaContent else deltaContent.toString()
                                        if (deltaText.isNotEmpty()) {
                                            emit(deltaText)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore parse errors for partial chunks
                                }
                            }
                        }
                    }
                } catch (e: retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    if (e.code() == 401) {
                        emit("Error: API Key 无效或未授权，请在设置中检查您的 API Key。")
                    } else {
                        emit("Error: HTTP ${e.code()} - $errorBody")
                    }
                } catch (e: java.net.UnknownHostException) {
                    emit("Error: 网络似乎断开了，但我仍在您的身边！您可以：\n1. 继续查阅本地错题本\n2. 回顾历史学习记录\n3. 等待网络恢复后，我将继续为您解答。")
                } catch (e: java.net.ConnectException) {
                    emit("Error: 网络连接失败，但我仍在您的身边！您可以：\n1. 继续查阅本地错题本\n2. 回顾历史学习记录\n3. 等待网络恢复后，我将继续为您解答。")
                } catch (e: java.net.SocketTimeoutException) {
                    emit("Error: 请求超时。网络可能不稳定，请稍后重试。")
                } catch (e: Exception) {
                    emit("Error: ${e.message}")
                }
            }
    }
