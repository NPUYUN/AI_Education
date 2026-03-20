package com.example.ai_tutor.data.repository

import com.example.ai_tutor.data.api.QwenService
import com.example.ai_tutor.data.model.ChatRequest
import com.example.ai_tutor.data.model.Message
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
class QwenRepository @Inject constructor(
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
        val api = RetrofitClient.create(apiKey, baseUrl).create(QwenService::class.java)
        // Construct messages: history + current user message
        val messages = history.toMutableList()
        
        val userContent: Any = if (imageUrl != null) {
            listOf(
                com.example.ai_tutor.data.model.ContentItem(type = "image_url", imageUrl = com.example.ai_tutor.data.model.ImageUrl(url = imageUrl)),
                com.example.ai_tutor.data.model.ContentItem(type = "text", text = prompt)
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
