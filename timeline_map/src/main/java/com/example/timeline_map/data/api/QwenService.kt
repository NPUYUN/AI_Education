package com.example.timeline_map.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface QwenService {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
