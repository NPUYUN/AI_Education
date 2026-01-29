package com.example.timeline_map.data.repository

import com.example.common.network.RetrofitClient
import com.example.timeline_map.data.api.ChatRequest
import com.example.timeline_map.data.api.Message
import com.example.timeline_map.data.api.Parameters
import com.example.timeline_map.data.api.QwenService
import com.example.timeline_map.data.model.HistoricalEvent
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.UUID

class TimelineRepository {
    suspend fun generateEvents(query: String, apiKey: String): Result<List<HistoricalEvent>> {
        return withContext(Dispatchers.IO) {
            try {
                val service = RetrofitClient.create(apiKey).create(QwenService::class.java)
                val prompt = """
                    你是历史事件整理助手。请根据问题生成事件列表。
                    输出严格 JSON 数组，每个元素包含：
                    time(字符串), location(字符串), description(字符串), people(字符串数组), latitude(数字), longitude(数字)。
                    不要输出其它文字或markdown。
                    问题：$query
                """.trimIndent()

                val request = ChatRequest(
                    model = "qwen-turbo",
                    messages = listOf(Message("user", prompt)),
                    parameters = Parameters("message")
                )
                val response = service.chat(request)
                val raw = response.choices?.firstOrNull()?.message?.content
                    ?: response.output?.choices?.firstOrNull()?.message?.content
                    ?: response.output?.text
                    ?: ""
                val text = if (raw is String) raw else raw.toString()
                val json = extractJsonArray(text) ?: return@withContext Result.failure(IllegalStateException("解析失败"))
                val parsed = parseEvents(json)
                if (parsed.isEmpty()) {
                    Result.failure(IllegalStateException("解析结果为空"))
                } else {
                    Result.success(parsed)
                }
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                val message = parseErrorMessage(body) ?: "请求失败 ${e.code()}"
                Result.failure(IllegalStateException(message))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun sampleEvents(): List<HistoricalEvent> {
        return listOf(
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = "1911-10-10",
                location = "武汉",
                description = "武昌起义爆发，辛亥革命序幕拉开",
                people = listOf("孙武", "蒋翊武", "刘复基"),
                latitude = 30.5928,
                longitude = 114.3055
            ),
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = "1911-10-12",
                location = "湖北",
                description = "湖北军政府成立，宣布与清政府脱离",
                people = listOf("黎元洪"),
                latitude = 30.5454,
                longitude = 114.3423
            ),
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = "1911-11-01",
                location = "长沙",
                description = "湖南响应起义，宣布独立",
                people = listOf("焦达峰", "陈作新"),
                latitude = 28.2282,
                longitude = 112.9388
            ),
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = "1911-12-25",
                location = "上海",
                description = "孙中山从海外回到上海筹组临时政府",
                people = listOf("孙中山"),
                latitude = 31.2304,
                longitude = 121.4737
            ),
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = "1912-01-01",
                location = "南京",
                description = "中华民国临时政府在南京成立，孙中山就任临时大总统",
                people = listOf("孙中山"),
                latitude = 32.0603,
                longitude = 118.7969
            )
        )
    }

    private fun extractJsonArray(text: String): String? {
        val cleaned = text.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        return cleaned.substring(start, end + 1)
    }

    private fun parseEvents(json: String): List<HistoricalEvent> {
        val element = JsonParser.parseString(json)
        if (!element.isJsonArray) return emptyList()
        return element.getAsJsonArray().mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val obj = item.getAsJsonObject()
            val time = obj.get("time")?.getAsString() ?: return@mapNotNull null
            val location = obj.get("location")?.getAsString() ?: return@mapNotNull null
            val description = obj.get("description")?.getAsString() ?: return@mapNotNull null
            val people = parsePeople(obj.get("people"))
            val latitude = obj.get("latitude")?.getAsDouble() ?: return@mapNotNull null
            val longitude = obj.get("longitude")?.getAsDouble() ?: return@mapNotNull null
            HistoricalEvent(
                id = UUID.randomUUID().toString(),
                time = time,
                location = location,
                description = description,
                people = people,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    private fun parsePeople(element: JsonElement?): List<String> {
        if (element == null) return emptyList()
        return when {
            element.isJsonArray -> element.getAsJsonArray().mapNotNull { it.getAsString() }
            element.isJsonPrimitive -> element.getAsString().split("、", "，", ",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun parseErrorMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val element = JsonParser.parseString(raw)
            val error = element.getAsJsonObject().getAsJsonObject("error")
            error?.get("message")?.getAsString()
        } catch (_: Exception) {
            null
        }
    }
}
