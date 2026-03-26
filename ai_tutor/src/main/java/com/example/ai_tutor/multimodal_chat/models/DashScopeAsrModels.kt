package com.example.ai_tutor.multimodal_chat.models

data class DashScopeAsrRequest(
    val model: String = "paraformer-realtime-v1",
    val input: AsrInput,
    val parameters: AsrParameters,
)

data class AsrInput(
    val audio: String,
)

data class AsrParameters(
    val format: String = "aac",
    val sample_rate: Int = 16000,
)

data class DashScopeAsrResponse(
    val request_id: String,
    val output: AsrOutput?,
    val code: String?,
    val message: String?,
)

data class AsrOutput(
    val sentence: AsrSentence?,
)

data class AsrSentence(
    val text: String,
    val begin_time: Long?,
    val end_time: Long?,
)
