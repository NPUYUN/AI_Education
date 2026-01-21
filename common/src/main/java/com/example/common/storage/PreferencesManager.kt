package com.example.common.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_USER_TOKEN = stringPreferencesKey("user_token")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_ID = androidx.datastore.preferences.core.longPreferencesKey("user_id")
        val KEY_LANGUAGE = stringPreferencesKey("app_language") // "zh" or "en"
    }

    val userToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[KEY_USER_TOKEN] }

    val userId: Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[KEY_USER_ID] }

    val language: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_LANGUAGE] ?: "zh" } // Default to Chinese

    suspend fun saveUserToken(token: String) {
        context.dataStore.edit { preferences -> preferences[KEY_USER_TOKEN] = token }
    }

    suspend fun saveUserId(id: Long) {
        context.dataStore.edit { preferences -> preferences[KEY_USER_ID] = id }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { preferences -> preferences[KEY_LANGUAGE] = lang }
    }

    suspend fun clearData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
