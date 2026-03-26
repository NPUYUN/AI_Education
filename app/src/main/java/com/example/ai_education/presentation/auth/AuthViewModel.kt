package com.example.ai_education.presentation.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.data.local.auth.UserDao
import com.example.common.data.local.auth.UserEntity
import com.example.common.database.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val userDao: UserDao,
        private val prefs: PreferencesManager,
    ) : ViewModel() {
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
                    prefs.saveString("user_nickname", user.nickname)
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
                val finalNick = if (nick.isBlank()) uid else nick
                val newUser = UserEntity(uid, finalNick, pwd)
                userDao.insertUser(newUser)
                prefs.saveString("current_user_id", newUser.id)
                prefs.saveString("user_nickname", finalNick)
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
