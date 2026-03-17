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

class QwenRepository(private val apiKey: String) {
    
    private val api: QwenService by lazy {
        RetrofitClient.create(apiKey).create(QwenService::class.java)
    }

    suspend fun sendMessage(
        prompt: String, 
        history: List<Message>, 
        imageUrl: String? = null
    ): Flow<String> = flow {
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
            model = if (imageUrl != null) "qwen-vl-max" else "qwen-turbo",
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
            val errorBody = e.response()?.errorBody()?.string()
            emit("Error: HTTP ${e.code()} - $errorBody")
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }
}
