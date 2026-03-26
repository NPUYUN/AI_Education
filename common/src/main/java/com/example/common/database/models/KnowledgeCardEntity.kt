package com.example.common.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_cards")
data class KnowledgeCardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val tags: String, // Comma separated tags
    val source: String, // e.g., "video", "text", "audio", "dialogue"
    val timestamp: Long = System.currentTimeMillis(),
)
