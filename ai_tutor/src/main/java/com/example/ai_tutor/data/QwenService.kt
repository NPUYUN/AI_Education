package com.example.ai_tutor.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

import okhttp3.ResponseBody
import retrofit2.http.Streaming

// Models
data class ChatCompletionRequest(
    val model: String = "qwen-vl-max",
    val messages: List<Message>,
    val stream: Boolean = false,
    val stream_options: StreamOptions? = null
)

data class StreamOptions(
    val include_usage: Boolean = true
)

data class Message(
    val role: String,
    val content: Any // Can be String or List<ContentPart>
)

data class ContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageResponse
)

data class MessageResponse(
    val role: String,
    val content: String
)

// API Interface
interface QwenApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @Streaming
    @POST("v1/chat/completions")
    suspend fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ResponseBody
}
