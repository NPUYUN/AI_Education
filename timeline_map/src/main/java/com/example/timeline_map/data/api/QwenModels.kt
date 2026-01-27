package com.example.timeline_map.data.api

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String = "qwen-turbo",
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("parameters") val parameters: Parameters? = null
)

data class Message(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any
)

data class Parameters(
    @SerializedName("result_format") val resultFormat: String = "message"
)

data class ChatResponse(
    @SerializedName("output") val output: Output?,
    @SerializedName("choices") val choices: List<Choice>?
)

data class Output(
    @SerializedName("text") val text: String?,
    @SerializedName("choices") val choices: List<Choice>?
)

data class Choice(
    @SerializedName("message") val message: Message
)
