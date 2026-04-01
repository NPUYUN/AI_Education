package com.example.review.planner.services

import com.example.common.dispatchers.DispatcherProvider
import com.example.common.network.llm.ChatMessage
import com.example.common.network.llm.ChatRequest
import com.example.common.network.llm.OpenAiService
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository
    @Inject
    constructor(
        private val dispatcherProvider: DispatcherProvider,
    ) {
        suspend fun generateReviewPlan(
            apiKey: String,
            baseUrl: String,
            modelName: String,
            subjects: String,
            recentContext: String,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val retrofit =
                        Retrofit.Builder()
                            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(
                                okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(60, TimeUnit.SECONDS)
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .writeTimeout(60, TimeUnit.SECONDS)
                                    .addInterceptor { chain ->
                                        val request =
                                            chain.request().newBuilder()
                                                .addHeader("Authorization", "Bearer $apiKey")
                                                .build()
                                        chain.proceed(request)
                                    }
                                    .build(),
                            )
                            .build()

                    val service = retrofit.create(OpenAiService::class.java)

                    val promptBuilder = StringBuilder()
                    promptBuilder.append("请为我生成一份结构化的复习计划。\n")
                    if (subjects.isNotBlank()) {
                        promptBuilder.append("我的需求或复习科目是：$subjects\n")
                    }
                    if (recentContext.isNotBlank()) {
                        promptBuilder.append("以下是我最近的学习记录（对话和错题），请结合这些内容找出我的薄弱点并纳入计划中：\n$recentContext\n")
                    }
                    promptBuilder.append("请按天列出未来一到两周的复习安排，包含复习轮次与间隔、任务类型（回顾/练习/测验/错题巩固）、产出要求与提醒。请严格按照计划格式输出，不要包含任何多余的废话，使用Markdown清晰呈现。")

                    val messages =
                        listOf(
                            ChatMessage(role = "system", content = "你是一个专业的AI学习规划师，基于艾宾浩斯记忆曲线为学生制定科学、高效、无废话的复习计划。"),
                            ChatMessage(role = "user", content = promptBuilder.toString()),
                        )

                    val request =
                        ChatRequest(
                            model = modelName,
                            messages = messages,
                        )

                    val response = service.chat(request)

                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
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
            knowledgePoint: String,
            recentContext: String,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val retrofit =
                        Retrofit.Builder()
                            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(
                                okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(60, TimeUnit.SECONDS)
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .writeTimeout(60, TimeUnit.SECONDS)
                                    .addInterceptor { chain ->
                                        val request =
                                            chain.request().newBuilder()
                                                .addHeader("Authorization", "Bearer $apiKey")
                                                .build()
                                        chain.proceed(request)
                                    }
                                    .build(),
                            )
                            .build()

                    val service = retrofit.create(OpenAiService::class.java)

                    val promptBuilder = StringBuilder()
                    promptBuilder.append("请帮我生成知识点总结和巩固练习。\n")
                    if (knowledgePoint.isNotBlank()) {
                        promptBuilder.append("我要巩固的知识点是：$knowledgePoint\n")
                    }
                    if (recentContext.isNotBlank()) {
                        promptBuilder.append("以下是我最近的学习记录（对话和错题），请从中提取关键知识点进行总结：\n$recentContext\n")
                    }
                    promptBuilder.append("请先清晰地总结核心知识点，然后生成3道选择题与1道简答或证明题，附详细答案解析与易错点提示，使用Markdown格式。")

                    val messages =
                        listOf(
                            ChatMessage(
                                role = "system",
                                content = "你是一个经验丰富的学科教师，善于从学生的学习记录中发现知识盲区，并提供针对性的知识点总结和高质量的巩固练习。",
                            ),
                            ChatMessage(role = "user", content = promptBuilder.toString()),
                        )

                    val request =
                        ChatRequest(
                            model = modelName,
                            messages = messages,
                        )

                    val response = service.chat(request)

                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
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

        suspend fun generateSimilarProblems(
            apiKey: String,
            baseUrl: String,
            modelName: String,
            sourceProblems: List<String>,
            count: Int,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val retrofit =
                        Retrofit.Builder()
                            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(
                                okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(60, TimeUnit.SECONDS)
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .writeTimeout(60, TimeUnit.SECONDS)
                                    .addInterceptor { chain ->
                                        val request =
                                            chain.request().newBuilder()
                                                .addHeader("Authorization", "Bearer $apiKey")
                                                .build()
                                        chain.proceed(request)
                                    }
                                    .build(),
                            )
                            .build()

                    val service = retrofit.create(OpenAiService::class.java)

                    val problemsStr = sourceProblems.joinToString("\n\n") { "原题：$it" }
                    val prompt = "请根据以下错题，生成 ${count} 道相似的练习题（注意难度和考点要相似，题型可以有选择题或解答题，题目分布要均匀）。请在最后附上这 ${count} 道题的详细答案解析。请使用Markdown格式输出。\n\n$problemsStr"

                    val messages =
                        listOf(
                            ChatMessage(role = "system", content = "你是一个专业的出题老师，擅长根据学生的错题生成高质量的相似变式题。"),
                            ChatMessage(role = "user", content = prompt),
                        )

                    val request = ChatRequest(model = modelName, messages = messages)
                    val response = service.chat(request)
                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
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

        suspend fun gradeTest(
            apiKey: String,
            baseUrl: String,
            modelName: String,
            problemsAndAnswers: String,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val retrofit =
                        Retrofit.Builder()
                            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(
                                okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(60, TimeUnit.SECONDS)
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .writeTimeout(60, TimeUnit.SECONDS)
                                    .addInterceptor { chain ->
                                        val request =
                                            chain.request().newBuilder()
                                                .addHeader("Authorization", "Bearer $apiKey")
                                                .build()
                                        chain.proceed(request)
                                    }
                                    .build(),
                            )
                            .build()

                    val service = retrofit.create(OpenAiService::class.java)

                    val prompt = "以下是学生做的一组题目及答案。请逐一判断对错，并给出批改意见和正确解析。请使用Markdown格式输出。\n\n$problemsAndAnswers"

                    val messages =
                        listOf(
                            ChatMessage(role = "system", content = "你是一个认真负责的阅卷老师，负责批改学生的练习题，判断正误并给出清晰的解析。"),
                            ChatMessage(role = "user", content = prompt),
                        )

                    val request = ChatRequest(model = modelName, messages = messages)
                    val response = service.chat(request)
                    val content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.text
                            ?: ""

                    if (content.isBlank()) {
                        Result.failure(Exception("批改结果为空"))
                    } else {
                        Result.success(content.trim())
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
