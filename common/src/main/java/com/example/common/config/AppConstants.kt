package com.example.common.config

object AppConstants {
    // API Configuration
    const val BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/" // Qwen API Base URL
    const val TIMEOUT_SECONDS = 30L
    const val DEFAULT_MODEL_NAME = "qwen-turbo"
    
    // Database Name
    const val DATABASE_NAME = "education_app_db"
    
    // Preference Name
    const val PREFERENCES_NAME = "app_preferences"
    
    // API Keys (Note: In production, these should be in BuildConfig or secure storage)
    const val DEFAULT_API_KEY = "" // Removed hardcoded key for security
    const val API_KEY_HEADER = "Authorization"
}
