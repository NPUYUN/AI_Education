package com.example.education

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.database.AppDatabase
import com.example.common.repository.UserRepository
import com.example.common.storage.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    val userRepository = UserRepository(database.userDao(), preferencesManager)

    val isLoggedIn: StateFlow<Boolean> = userRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentLanguage: StateFlow<String> = userRepository.currentLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "zh")

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            userRepository.setLanguage(lang)
        }
    }
}
