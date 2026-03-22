package com.example.ai_tutor.multimodal_chat.services

import com.example.common.network.llm.ChatMessage

// Entity for Dialogue Context
data class DialogueContext(
    val sessionId: String,
    val history: MutableList<ChatMessage> = mutableListOf(),
    val currentIntent: String? = null
)
