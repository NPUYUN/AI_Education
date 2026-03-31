package com.example.summarizer.dialogue_summarizer.services

import com.example.common.database.models.MessageEntity
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.RetrofitClient
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogueSummaryRepository
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) {
        suspend fun summarizeDialogue(
            apiKey: String,
            messages: List<MessageEntity>,
            modelName: String,
            baseUrl: String,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    if (messages.isEmpty()) {
                        return@withContext Result.failure(Exception("对话内容为空"))
                    }

                    // Format dialogue history
                    val dialogueText =
                        messages.joinToString("\n") {
                            val role = if (it.role == "user") "User" else "AI Tutor"
                            "[$role]: ${it.content}"
                        }

                    val service = RetrofitClient.create(apiKey, baseUrl).create(OpenAiService::class.java)
                    val request =
                        ChatRequest(
                            model = modelName,
                            messages =
                                listOf(
                                    ChatMessage(
                                        role = "system",
                                        content = com.example.common.config.AppConstants.DIALOGUE_SUMMARY_SYSTEM_PROMPT,
                                    ),
                                    ChatMessage(
                                        role = "user",
                                        content = "以下是对话记录：\n\n$dialogueText",
                                    ),
                                ),
                        )

                    val response = service.chat(request)
                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.text
                            ?: ""

                    if (content.isBlank()) {
                        Result.failure(Exception("总结结果为空"))
                    } else {
                        Result.success(content.trim())
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
