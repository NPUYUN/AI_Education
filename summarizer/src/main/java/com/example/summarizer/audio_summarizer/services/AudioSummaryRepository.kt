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
                    ChatMessage(
                        role = "system",
                        content = "你是一个专业的音频/语音总结助手。请对用户提供的语音转写文本进行深度解析和层次化总结。\n" +
                                "由于是语音转写，可能存在少许错别字或口语化表达，请在理解时自动修正或忽略。\n" +
                                "总结必须包含以下结构：\n" +
                                "1. 核心主旨：用一段话概括音频的核心内容。\n" +
                                "2. 关键信息：分点列出音频中提到的核心要点或结论。\n" +
                                "3. 详细内容：补充关键的支持性细节、例子或讨论。\n" +
                                "4. 行动项/建议（可选）：如果音频中提到了需要执行的任务或建议，请列出。\n" +
                                "请使用清晰的Markdown格式输出，可以适当使用加粗和列表。"
                    ),
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
