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
            sourceProblems: List<com.example.common.database.models.ErrorBookEntity>,
            count: Int,
        ): Result<List<com.example.review.planner.models.GeneratedProblem>> =
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

                    val problemsStr = sourceProblems.joinToString("\n\n") { "原题内容：${it.questionContent}\n知识点/错误原因：${it.errorReason}" }
                    val prompt =
                        """
请严格根据以下错题信息，生成 $count 道相似变式题。
必须保证生成题目与原题高度相关，向量相似度阈值预估要求达到 0.85 以上。

必须以 JSON 格式返回结果，返回结构必须如下：
{
  "problems": [
    {
      "questionText": "题干内容...",
      "options": ["A. 选项", "B. 选项", "C. 选项", "D. 选项"], // 如果是解答题可为 null 或空数组
      "answer": "正确答案",
      "explanation": "详细解析",
      "knowledgePointId": "原题知识点标签",
      "difficulty": "难度(如:中等,困难)",
      "questionType": "题型(选择题/解答题)",
      "similarityScore": 0.90 // 必须是一个0到1之间的数字，预估与原题的相似度
    }
  ]
}

原题信息：
$problemsStr
                        """.trimIndent()

                    val messages =
                        listOf(
                            ChatMessage(role = "system", content = "你是一个专业的出题老师，仅输出纯JSON格式，不要包裹在 ```json 中。"),
                            ChatMessage(role = "user", content = prompt),
                        )

                    val request = ChatRequest(model = modelName, messages = messages, responseFormat = mapOf("type" to "json_object"))
                    val response = service.chat(request)
                    var content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.text
                            ?: ""

                    // Clean markdown fences if any
                    content = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                    if (content.isBlank()) {
                        Result.failure(Exception("生成结果为空"))
                    } else {
                        val parsed =
                            com.google.gson.Gson().fromJson(
                                content,
                                com.example.review.planner.models.GenerateProblemsResponse::class.java,
                            )
                        if (parsed.problems.any { (it.similarityScore ?: 0.0) < 0.85 }) {
                            Result.failure(Exception("生成题目相关性过低，请稍后重试"))
                        } else {
                            Result.success(parsed.problems)
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        suspend fun gradeTest(
            apiKey: String,
            baseUrl: String,
            modelName: String,
            problemsAndAnswers: List<Pair<com.example.review.planner.models.GeneratedProblem, String>>,
        ): Result<List<com.example.review.planner.presentation.viewmodels.PracticeGradingResult>> =
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

                    val problemsStr =
                        problemsAndAnswers.mapIndexed { index, (problem, answer) ->
                            "第${index + 1}题：\n【题目内容】\n${problem.questionText}\n【标准答案】\n${problem.answer}\n【学生答案】\n$answer"
                        }.joinToString("\n\n---\n\n")

                    val prompt =
                        """
以下是学生做的一组题目及答案。请逐一判断对错，并给出批改意见和正确解析。
请以 JSON 格式返回结果，返回结构必须如下：
{
  "results": [
    {
      "isCorrect": true, // 是否正确
      "score": 100, // 此题得分，满分100
      "explanation": "详细批改意见和解析..."
    }
  ]
}

题目及作答：
$problemsStr
                        """.trimIndent()

                    val messages =
                        listOf(
                            ChatMessage(role = "system", content = "你是一个认真负责的阅卷老师，负责批改学生的练习题。仅输出纯JSON格式，不要包裹在 ```json 中。"),
                            ChatMessage(role = "user", content = prompt),
                        )

                    val request = ChatRequest(model = modelName, messages = messages, responseFormat = mapOf("type" to "json_object"))
                    val response = service.chat(request)
                    var content =
                        response.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.choices?.firstOrNull()?.message?.content?.toString()
                            ?: response.output?.text
                            ?: ""

                    // Clean markdown fences if any
                    content = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                    if (content.isBlank()) {
                        Result.failure(Exception("批改结果为空"))
                    } else {
                        val parsed =
                            com.google.gson.Gson().fromJson(
                                content,
                                com.example.review.planner.models.GradeTestResponse::class.java,
                            )
                        Result.success(parsed.results)
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
