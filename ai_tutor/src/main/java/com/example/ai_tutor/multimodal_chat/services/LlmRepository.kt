package com.example.ai_tutor.multimodal_chat.services

import com.example.common.network.llm.OpenAiService
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.ChatMessage as Message
import com.example.common.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

import javax.inject.Inject
import javax.inject.Singleton
import com.example.common.config.AppConstants
import com.example.common.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext

@Singleton
class LlmRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    suspend fun sendMessage(
        apiKey: String,
        baseUrl: String = AppConstants.BASE_URL,
        modelName: String = AppConstants.DEFAULT_MODEL_NAME,
        prompt: String, 
        history: List<Message>, 
        imageUrl: String? = null
    ): Flow<String> = flow {
        val api = RetrofitClient.create(apiKey, baseUrl).create(OpenAiService::class.java)
        
        val messages = mutableListOf<Message>()
        
        // 注入专业的 System Prompt 以提升 AI 导师的回复质量
        val hasSystemPrompt = history.any { it.role == "system" }
        if (!hasSystemPrompt) {
            val systemPrompt = """
                你是一个专业、耐心且富有启发性的全能AI智能导师。你的目标是帮助用户高效学习、深入理解并解决各种问题。
                请遵循以下辅导原则：
                1. 专业与精确：提供的信息必须准确无误、符合事实，涉及专业领域时请使用规范术语。
                2. 启发与引导：当用户询问复杂问题或寻求解答时，不要只是直接给出最终答案，而应适当通过反问或提示，引导用户自己思考和推导。
                3. 清晰与结构化：回答要条理分明，逻辑清晰。对于复杂的解答，请使用分段、列表、加粗等方式进行排版，代码请使用 Markdown 格式。
                4. 温和与鼓励：保持友好、耐心和同理心，多给予用户正面的反馈和鼓励，建立积极的学习氛围。
                5. 针对性：严格根据用户的具体问题和上下文进行回复，直接切中要害，避免长篇大论与问题无关的内容。
            """.trimIndent()
            messages.add(Message("system", systemPrompt))
        }
        
        // 拼接历史记录和当前用户消息
        messages.addAll(history)
        
        val userContent: Any = if (imageUrl != null) {
            listOf(
                com.example.common.network.llm.ContentItem(type = "image_url", imageUrl = com.example.common.network.llm.ImageUrl(url = imageUrl)),
                com.example.common.network.llm.ContentItem(type = "text", text = prompt)
            )
        } else {
            prompt
        }
        
        messages.add(Message("user", userContent))
        
        val request = ChatRequest(
            model = if (imageUrl != null && modelName.startsWith("qwen-turbo")) "qwen-vl-max" else modelName,
            messages = messages
        )

        try {
            val response = api.chat(request)
            // Handle both Native (output.text/choices) and OpenAI-compatible (root choices) formats
            val reply = response.choices?.firstOrNull()?.message?.content
                ?: response.output?.choices?.firstOrNull()?.message?.content 
                ?: response.output?.text 
                ?: ""
            
            // Handle content being Any? (Usually String in response)
            val replyText = if (reply is String) reply else reply.toString()
            
            emit(replyText)
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            if (e.code() == 401) {
                emit("Error: API Key 无效或未授权，请在设置中检查您的 API Key。")
            } else {
                emit("Error: HTTP ${e.code()} - $errorBody")
            }
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }
}
