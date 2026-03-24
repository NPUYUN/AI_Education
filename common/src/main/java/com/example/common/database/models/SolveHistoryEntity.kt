package com.example.common.database.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solve_history")
data class SolveHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String, // 几何、代数、综合
    val questionContent: String,
    val imageUri: String?, // 可为空
    val solution: String,
    val isInErrorBook: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
