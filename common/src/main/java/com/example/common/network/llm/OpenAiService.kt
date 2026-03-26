package com.example.common.network.llm

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OpenAiService {
    @POST("chat/completions")
    suspend fun chat(
        @Body request: ChatRequest,
    ): ChatResponse

    @Streaming
    @POST("chat/completions")
    suspend fun chatStream(
        @Body request: ChatRequest,
    ): ResponseBody
}
