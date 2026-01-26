package com.example.ai_tutor.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String = "qwen-turbo",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("parameters") val parameters: Parameters? = null
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any // String or List<ContentItem>
)

data class ContentItem(
    @SerializedName("type") val type: String, // "text" or "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url") val url: String
)

data class Parameters(
    @SerializedName("result_format") val resultFormat: String = "message"
)

data class ChatResponse(
    @SerializedName("output") val output: Output?,
    @SerializedName("usage") val usage: Usage?,
    @SerializedName("request_id") val requestId: String?
)

data class Output(
    @SerializedName("text") val text: String?,
    @SerializedName("finish_reason") val finishReason: String?,
    @SerializedName("choices") val choices: List<Choice>?
)

data class Choice(
    @SerializedName("message") val message: Message,
    @SerializedName("finish_reason") val finishReason: String
)

data class Usage(
    @SerializedName("total_tokens") val totalTokens: Int,
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)
