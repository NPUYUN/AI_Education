package com.example.common.network

import com.example.common.config.AppConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val MAX_CACHE_SIZE = 5
    private val clients =
        object : LinkedHashMap<String, Retrofit>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Retrofit>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }

    @Synchronized
    fun create(
        apiKey: String,
        baseUrl: String = AppConstants.BASE_URL,
        timeoutSeconds: Long = AppConstants.TIMEOUT_SECONDS,
    ): Retrofit {
        val cacheKey = "${baseUrl}_${apiKey}_$timeoutSeconds"

        var retrofit = clients[cacheKey]
        if (retrofit == null) {
            val logging =
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

            val client =
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(AuthInterceptor(apiKey))
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build()

            retrofit =
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

            clients[cacheKey] = retrofit
        }
        return retrofit
    }

    @Synchronized
    fun clearCache() {
        clients.clear()
    }
}
