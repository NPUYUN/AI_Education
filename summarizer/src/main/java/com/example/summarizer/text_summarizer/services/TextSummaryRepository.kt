package com.example.summarizer.text_summarizer.services

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
class TextSummaryRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun summarizeText(
        apiKey: String,
        text: String,
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
                        content = "你是一个专业的文本总结助手。请对用户提供的文本进行深度解析和层次化总结。\n" +
                                "总结必须包含以下结构：\n" +
                                "1. 概述：用一段话概括核心内容。\n" +
                                "2. 要点：分点列出文本的核心要点。\n" +
                                "3. 细节：补充关键的支持性细节。\n" +
                                "4. 关键词：提取3-5个核心术语或概念。\n" +
                                "请使用清晰的Markdown格式输出，可以适当使用加粗和列表。"
                    ),
                    ChatMessage(
                        role = "user",
                        content = "请对以下文本进行总结：\n\n$text"
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
