package com.example.summarizer.audio_summarizer.services

import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.RetrofitClient
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.ChatParameters
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSummaryRepository
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) {
        suspend fun summarizeAudioTranscript(
            apiKey: String,
            transcript: String,
            modelName: String,
            baseUrl: String,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val service = RetrofitClient.create(apiKey, baseUrl).create(OpenAiService::class.java)
                    val request =
                        ChatRequest(
                            model = modelName,
                            messages =
                                listOf(
                                    ChatMessage(
                                        role = "system",
                                        content = com.example.common.config.AppConstants.AUDIO_SUMMARY_SYSTEM_PROMPT,
                                    ),
                                    ChatMessage(
                                        role = "user",
                                        content = "请对以下语音转写文本进行总结：\n\n$transcript",
                                    ),
                                ),
                            parameters = ChatParameters(),
                        )
                    val response = service.chat(request)
                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.text
                            ?: ""

                    if (content.isBlank()) {
                        Result.failure(Exception("摘要生成失败，返回内容为空"))
                    } else {
                        Result.success(content.trim())
                    }
                } catch (e: java.net.UnknownHostException) {
                    Result.failure(Exception("网络连接断开，请检查网络后重试。您可以先使用离线总结功能（如果有）或复习本地知识卡片。", e))
                } catch (e: java.net.ConnectException) {
                    Result.failure(Exception("网络连接失败，请检查网络后重试。", e))
                } catch (e: java.net.SocketTimeoutException) {
                    Result.failure(Exception("请求超时。网络可能不稳定，请稍后重试。", e))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
