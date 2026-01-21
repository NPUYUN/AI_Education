package com.example.timeline_map.data

data class HistoricalEvent(
    val id: String,
    val title: String,
    val year: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double
)
