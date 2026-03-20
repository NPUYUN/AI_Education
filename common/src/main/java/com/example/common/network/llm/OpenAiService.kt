package com.example.common.network.llm

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiService {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
