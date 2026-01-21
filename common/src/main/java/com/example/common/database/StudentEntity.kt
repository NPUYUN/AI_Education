package com.example.common.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_records")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val content: String,
    val timestamp: Long
)
