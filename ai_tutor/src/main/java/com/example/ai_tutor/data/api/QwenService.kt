package com.example.ai_tutor.data.api

import com.example.ai_tutor.data.model.ChatRequest
import com.example.ai_tutor.data.model.ChatResponse
import com.example.ai_tutor.data.model.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface QwenService {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
