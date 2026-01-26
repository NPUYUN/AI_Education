package com.example.common.network

import com.example.common.config.AppConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    fun create(apiKey: String, baseUrl: String = AppConstants.BASE_URL): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(apiKey))
            .connectTimeout(AppConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
