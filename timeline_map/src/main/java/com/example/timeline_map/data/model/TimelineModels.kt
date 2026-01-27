package com.example.timeline_map.data.model

data class HistoricalEvent(
    val id: String,
    val time: String,
    val location: String,
    val description: String,
    val people: List<String>,
    val latitude: Double,
    val longitude: Double,
    val relatedIds: List<String> = emptyList()
)

enum class SpeechLanguage(val tag: String) {
    AUTO(""),
    ZH("zh-CN"),
    EN("en-US")
}
