package com.example.ai_tutor.presentation.auth

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tutor.data.local.AiTutorDatabase
import com.example.ai_tutor.data.local.entity.UserEntity
import com.example.common.database.PreferencesManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AiTutorDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val prefs = PreferencesManager(application)

    val username = mutableStateOf("")
    val password = mutableStateOf("")
    val nickname = mutableStateOf("")
    val error = mutableStateOf<String?>(null)
    val isLoggedIn = mutableStateOf(false)

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            prefs.getString("current_user_id").collect { userId ->
                isLoggedIn.value = userId.isNotEmpty()
            }
        }
    }

    fun login(onSuccess: () -> Unit) {
        val uid = username.value
        val pwd = password.value
        if (uid.isBlank() || pwd.isBlank()) {
            error.value = "Username and password required"
            return
        }
        viewModelScope.launch {
            val user = userDao.getUser(uid)
            if (user != null && user.passwordHash == pwd) { // In real app, hash pwd
                prefs.saveString("current_user_id", user.id)
                onSuccess()
            } else {
                error.value = "Invalid credentials"
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val uid = username.value
        val pwd = password.value
        val nick = nickname.value
        if (uid.isBlank() || pwd.isBlank()) {
            error.value = "All fields required"
            return
        }
        viewModelScope.launch {
            if (userDao.getUser(uid) != null) {
                error.value = "User already exists"
                return@launch
            }
            val newUser = UserEntity(uid, if (nick.isBlank()) uid else nick, pwd)
            userDao.insertUser(newUser)
            prefs.saveString("current_user_id", newUser.id)
            onSuccess()
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            prefs.saveString("current_user_id", "")
            isLoggedIn.value = false
        }
    }
}
