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
class AudioSummaryRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun summarizeAudioTranscript(
        apiKey: String,
        transcript: String,
        modelName: String,
        baseUrl: String
    ): Result<String> = withContext(dispatcherProvider.io) {
        try {
            val service = RetrofitClient.create(apiKey, baseUrl).create(OpenAiService::class.java)
            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(role = "system", content = com.example.common.config.AppConstants.AUDIO_SUMMARY_SYSTEM_PROMPT),
                    ChatMessage(
                        role = "user",
                        content = "请对以下语音转写文本进行总结：\n\n$transcript"
                    )
                ),
                parameters = ChatParameters()
            )
            val response = service.chat(request)
            val content = response.choices?.firstOrNull()?.message?.content?.toString()
                ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                ?: response.output?.text
                ?: ""

            if (content.isBlank()) {
                Result.failure(Exception("摘要生成失败，返回内容为空"))
            } else {
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
