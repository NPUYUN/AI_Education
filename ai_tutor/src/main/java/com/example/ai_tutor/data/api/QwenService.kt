package com.example.ai_tutor.data.api

import com.example.ai_tutor.data.model.ChatRequest
import com.example.ai_tutor.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface QwenService {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
