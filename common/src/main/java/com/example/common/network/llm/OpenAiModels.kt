package com.example.common.network.llm

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("parameters") val parameters: ChatParameters? = null,
    @SerializedName("stream") val stream: Boolean = false,
)

data class ChatMessage(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: Any? = null, // Can be String or List<ContentItem>
)

data class ContentItem(
    @SerializedName("type") val type: String, // "text" or "image_url"
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null,
)

data class ImageUrl(
    @SerializedName("url") val url: String,
)

data class ChatParameters(
    @SerializedName("result_format") val resultFormat: String = "message",
)

data class ChatResponse(
    @SerializedName("output") val output: ChatOutput?,
    @SerializedName("choices") val choices: List<ChatChoice>?,
    @SerializedName("usage") val usage: ChatUsage?,
    @SerializedName("request_id") val requestId: String?,
)

data class ChatOutput(
    @SerializedName("text") val text: String?,
    @SerializedName("finish_reason") val finishReason: String?,
    @SerializedName("choices") val choices: List<ChatChoice>?,
)

data class ChatChoice(
    @SerializedName("message") val message: ChatMessage? = null,
    @SerializedName("delta") val delta: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

data class ChatUsage(
    @SerializedName("total_tokens") val totalTokens: Int,
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int,
)
