package com.example.common.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_history")
data class ReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "planner", "reinforcement", "practice_generation", "practice_grading"
    val inputParameters: String, // JSON or formatted string of inputs (e.g., subjects, knowledge points)
    val resultContent: String, // The generated markdown content
    val timestamp: Long = System.currentTimeMillis(),
)
