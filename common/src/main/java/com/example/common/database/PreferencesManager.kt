package com.example.common.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.common.config.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = AppConstants.PREFERENCES_NAME)

class PreferencesManager(private val context: Context) {
    suspend fun saveString(
        key: String,
        value: String,
    ) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[dataStoreKey] = value
        }
    }

    fun getString(
        key: String,
        defaultValue: String = "",
    ): Flow<String> {
        val dataStoreKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }
    }

    suspend fun saveBoolean(
        key: String,
        value: Boolean,
    ) {
        val dataStoreKey = booleanPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[dataStoreKey] = value
        }
    }

    fun getBoolean(
        key: String,
        defaultValue: Boolean = false,
    ): Flow<Boolean> {
        val dataStoreKey = booleanPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[dataStoreKey] ?: defaultValue
        }
    }
}
