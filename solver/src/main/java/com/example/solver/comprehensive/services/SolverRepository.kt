package com.example.solver.comprehensive.services

import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.ContentItem
import com.example.common.network.llm.ImageUrl
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

import java.util.concurrent.TimeUnit

@Singleton
class SolverRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {

    suspend fun solveProblem(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        systemPrompt: String,
        questionText: String,
        base64Image: String?
    ): Result<String> = withContext(dispatcherProvider.io) {
        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    okhttp3.OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val request = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer $apiKey")
                                .build()
                            chain.proceed(request)
                        }
                        .build()
                )
                .build()

            val service = retrofit.create(OpenAiService::class.java)

            val messages = mutableListOf<ChatMessage>()
            
            // Add system prompt
            messages.add(
                ChatMessage(
                    role = "system",
                    content = systemPrompt
                )
            )

            // Construct user content
            val userContent: Any = if (base64Image != null) {
                listOf(
                    ContentItem(type = "text", text = questionText.takeIf { it.isNotBlank() } ?: "请解析这道题。"),
                    ContentItem(type = "image_url", imageUrl = ImageUrl(url = base64Image))
                )
            } else {
                questionText
            }

            messages.add(
                ChatMessage(
                    role = "user",
                    content = userContent
                )
            )

            val request = ChatRequest(
                model = modelName,
                messages = messages
            )

            val response = service.chat(request)
            
            val content = response.choices?.firstOrNull()?.message?.content?.toString()
                ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                ?: response.output?.text
                ?: ""

            if (content.isBlank()) {
                Result.failure(Exception("解题结果为空"))
            } else {
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
