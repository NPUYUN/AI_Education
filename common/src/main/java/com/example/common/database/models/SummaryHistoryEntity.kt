package com.example.common.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary_history")
data class SummaryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "text", "audio", "dialogue", "video"
    val sourceTitle: String, // E.g., file name, or first few words of input
    val summaryResult: String,
    val timestamp: Long = System.currentTimeMillis(),
)
