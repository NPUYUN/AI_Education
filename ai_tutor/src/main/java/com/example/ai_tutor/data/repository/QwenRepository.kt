package com.example.ai_tutor.data.repository

import com.example.ai_tutor.data.api.QwenService
import com.example.ai_tutor.data.model.ChatRequest
import com.example.ai_tutor.data.model.Message
import com.example.common.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QwenRepository(private val apiKey: String) {
    
    private val api: QwenService by lazy {
        RetrofitClient.create(apiKey).create(QwenService::class.java)
    }

    suspend fun sendMessage(
        content: String, 
        history: List<Message> = emptyList(),
        model: String = "qwen-turbo", // or qwen-vl-max for vision
        imageUrl: String? = null // Base64 data:image/jpeg;base64,...
    ): Flow<String> = flow {
        // Construct messages: history + current user message
        val messages = history.toMutableList()
        
        val userContent: Any = if (imageUrl != null) {
            listOf(
                com.example.ai_tutor.data.model.ContentItem(type = "image_url", imageUrl = com.example.ai_tutor.data.model.ImageUrl(url = imageUrl)),
                com.example.ai_tutor.data.model.ContentItem(type = "text", text = content)
            )
        } else {
            content
        }
        
        messages.add(Message("user", userContent))
        
        val request = ChatRequest(
            model = if (imageUrl != null) "qwen-vl-max" else model,
            messages = messages
        )

        try {
            val response = api.chat(request)
            // Qwen API structure usually returns content in output.text or choices[0].message.content
            // Based on model documentation (qwen-turbo), it returns output.text or choices
            val reply = response.output?.choices?.firstOrNull()?.message?.content 
                ?: response.output?.text 
                ?: ""
            
            // Handle content being Any? (Usually String in response)
            val replyText = if (reply is String) reply else reply.toString()
            
            emit(replyText)
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }
}
