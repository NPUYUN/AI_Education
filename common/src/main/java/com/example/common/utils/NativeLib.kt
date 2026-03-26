package com.example.common.utils

import com.example.common.BuildConfig

object NativeLib {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("ai_education_native")
            isNativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
            isNativeLoaded = false
        }
    }

    /**
     * A native method that is implemented by the 'ai_education_native' native library,
     * which is packaged with this application.
     */
    private external fun getApiKeyNative(): String

    fun getApiKey(): String {
        return if (isNativeLoaded) {
            getApiKeyNative()
        } else {
            // Fallback to BuildConfig if NDK library is not compiled/available
            BuildConfig.API_KEY
        }
    }
}
