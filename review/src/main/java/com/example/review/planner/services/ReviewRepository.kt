package com.example.review.planner.services

import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

import java.util.concurrent.TimeUnit

@Singleton
class ReviewRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {

    suspend fun generateReviewPlan(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        subjects: List<String>
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

            val subjectStr = subjects.joinToString("、")
            val prompt = "请基于艾宾浩斯记忆曲线为以下科目生成结构化复习计划：$subjectStr。请按天列出未来一到两周的复习安排，包含复习轮次与间隔、任务类型（回顾/练习/测验/错题巩固）、产出要求与提醒，使用Markdown清晰呈现。"

            val messages = listOf(
                ChatMessage(role = "system", content = com.example.common.config.AppConstants.REVIEW_PLANNER_SYSTEM_PROMPT),
                ChatMessage(role = "user", content = prompt)
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
                Result.failure(Exception("生成结果为空"))
            } else {
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateReinforcementQuiz(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        knowledgePoint: String
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

            val prompt = "需要巩固知识点：$knowledgePoint。请生成3道选择题与1道简答或证明题，附详细答案解析与易错点提示，使用Markdown格式。"

            val messages = listOf(
                ChatMessage(role = "system", content = com.example.common.config.AppConstants.REVIEW_REINFORCEMENT_SYSTEM_PROMPT),
                ChatMessage(role = "user", content = prompt)
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
                Result.failure(Exception("生成结果为空"))
            } else {
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
