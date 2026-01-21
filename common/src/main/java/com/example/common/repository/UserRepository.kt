package com.example.common.repository

import com.example.common.database.UserDao
import com.example.common.database.UserEntity
import com.example.common.storage.PreferencesManager
import com.example.common.utils.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserRepository(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) {

    // Login: Verify credentials and save session
    suspend fun login(username: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.getUserByUsername(username)
            if (user != null) {
                // In a real app, use salt + hash. Here we used simple SHA256 in CryptoUtils or similar
                // Assuming passwordHash in DB is SHA256 of plain password for this demo
                val inputHash = CryptoUtils.sha256(password)
                if (user.passwordHash == inputHash) {
                    preferencesManager.saveUserId(user.id)
                    preferencesManager.saveUserToken("dummy_token_${user.id}") // Mock token
                    // Also sync language preference
                    preferencesManager.saveLanguage(user.preferredLanguage)
                    Result.success(user)
                } else {
                    Result.failure(Exception("Invalid password"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Register: Create new user
    suspend fun register(username: String, password: String, email: String?): Result<Long> {
        return try {
            val existing = userDao.getUserByUsername(username)
            if (existing != null) {
                return Result.failure(Exception("Username already exists"))
            }
            val passwordHash = CryptoUtils.sha256(password)
            val newUser = UserEntity(
                username = username,
                passwordHash = passwordHash,
                email = email
            )
            val id = userDao.insertUser(newUser)
            // Auto login after register?
            // preferencesManager.saveUserId(id)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout: Clear session
    suspend fun logout() {
        preferencesManager.clearData()
        // Reset to default language or keep? usually keep.
        // But clearData clears everything. Let's modify clearData or re-save language.
        // For now, let's assume logout clears user session but maybe we want to keep language.
        // Actually clearData clears ALL preferences. We might want to keep language.
        // Let's refactor clearData in PreferencesManager later if needed.
    }

    // Language
    val currentLanguage: Flow<String> = preferencesManager.language

    suspend fun setLanguage(lang: String) {
        preferencesManager.saveLanguage(lang)
        // If logged in, update user profile in DB too
        val userId = preferencesManager.userId.first()
        if (userId != null) {
            userDao.updateLanguage(userId, lang)
        }
    }

    // Session
    val isLoggedIn: Flow<Boolean> = preferencesManager.userId.map { it != null }
    
    val currentUser: Flow<UserEntity?> = preferencesManager.userId.map { id ->
        if (id != null) userDao.getUserById(id).first() else null
    }
}
