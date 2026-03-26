package com.example.ai_tutor.timeline_map.models

data class HistoricalEvent(
    val id: String,
    val time: String,
    val location: String,
    val description: String,
    val people: List<String>,
    val latitude: Double,
    val longitude: Double,
    val linkedEventIds: List<String> = emptyList(),
)

enum class SpeechLanguage(val tag: String) {
    AUTO(""),
    ZH("zh-CN"),
    EN("en-US"),
}

// Basic entity for Knowledge Graph
data class KnowledgePoint(
    val id: String,
    val name: String,
    val subject: String,
    val description: String,
    val relatedPoints: List<String> = emptyList(), // IDs of related points
)
