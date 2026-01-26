package com.example.ai_tutor.domain

import com.example.ai_tutor.data.model.Message

// Basic entity for Knowledge Graph
data class KnowledgePoint(
    val id: String,
    val name: String,
    val subject: String,
    val description: String,
    val relatedPoints: List<String> = emptyList() // IDs of related points
)

// Entity for Dialogue Context
data class DialogueContext(
    val sessionId: String,
    val history: MutableList<Message> = mutableListOf(),
    val currentIntent: String? = null
)
