package com.example.common.data.local.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // UUID or username
    val nickname: String,
    val passwordHash: String, // Simple hash for demo
    val createdAt: Long = System.currentTimeMillis(),
)
