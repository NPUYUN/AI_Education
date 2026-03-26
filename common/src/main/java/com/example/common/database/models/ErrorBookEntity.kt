package com.example.common.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "error_book")
data class ErrorBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val questionContent: String,
    val errorReason: String,
    val correctSolution: String,
    val timestamp: Long = System.currentTimeMillis(),
)
