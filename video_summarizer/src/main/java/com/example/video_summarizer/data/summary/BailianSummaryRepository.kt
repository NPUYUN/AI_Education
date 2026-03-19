package com.example.video_summarizer.data.summary

import com.example.common.network.RetrofitClient
import com.example.video_summarizer.data.asr.SherpaAsrManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.NetworkInterface

private const val ASR_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/"

data class AsrRequest(
    val model: String,
    val input: AsrInput,
    val parameters: AsrParameters? = null
)

data class AsrInput(
    val file_urls: List<String>
)

data class AsrParameters(
    val language_hints: List<String>? = null
)

data class AsrSubmitResponse(
    val output: AsrTaskOutput?,
    val request_id: String? = null,
    val code: String? = null,
    val message: String? = null
)

data class AsrTaskOutput(
    val task_id: String?,
    val task_status: String?
)

data class AsrTaskResponse(
    val output: AsrTaskResultOutput?,
    val request_id: String? = null,
    val code: String? = null,
    val message: String? = null
)

data class AsrTaskResultOutput(
    val task_status: String?,
    val results: List<AsrResultItem>?
)

data class AsrResultItem(
    val transcription_url: String?,
    val subtask_status: String? = null
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val parameters: ChatParameters? = null
)

data class ChatMessage(
    val role: String,
    val content: Any
)

data class ChatParameters(
    val result_format: String = "message"
)

data class ChatResponse(
    val output: ChatOutput? = null,
    val choices: List<ChatChoice>? = null
)

data class ChatOutput(
    val text: String? = null,
    val choices: List<ChatChoice>? = null
)

data class ChatChoice(
    val message: ChatMessage? = null
)

interface BailianAsrService {
    @POST("services/audio/asr/transcription")
    suspend fun submitTranscription(
        @Body request: AsrRequest,
        @Header("X-DashScope-Async") async: String = "enable",
        @Header("X-DashScope-OssResourceResolve") ossResolve: String = "enable"
    ): AsrSubmitResponse

    @GET("tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): AsrTaskResponse
}

interface BailianChatService {
    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

class BailianSummaryRepository(private val sherpaAsrManager: SherpaAsrManager? = null) {

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var cachedEndpoint: String? = null

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }

    private suspend fun scanLocalNetwork(): String? = withContext(Dispatchers.IO) {
        // 1. 优先检查缓存
        cachedEndpoint?.let { return@withContext it }

        // 2. 构造高优先级列表 (本机, 模拟器宿主)
        val highPriority = listOf(
            "127.0.0.1",
            "localhost",
            "10.0.2.2" // Android 模拟器访问宿主
        ).flatMap { host ->
            listOf(
                "http://$host:10095",
                "http://$host:10096"
            )
        }

        // 3. 并发检查高优先级
        val found = checkEndpoints(highPriority, 500)
        if (found != null) {
            cachedEndpoint = found
            return@withContext found
        }

        // 4. 构造局域网列表
        val localIp = getLocalIpAddress()
        if (localIp != null) {
            val prefix = localIp.substringBeforeLast(".")
            // 扫描同网段 1-254 (排除本机已扫过的)
            val lanTargets = (1..254).mapNotNull { i ->
                val ip = "$prefix.$i"
                if (ip == localIp) null else ip
            }.flatMap { ip ->
                listOf(
                    "http://$ip:10095",
                    "http://$ip:10096"
                )
            }
            
            // 5. 并发检查局域网 (分批处理以避免资源耗尽)
            // 每次并发 40 个，超时 500ms
            lanTargets.chunked(40).forEach { batch ->
                val lanFound = checkEndpoints(batch, 500)
                if (lanFound != null) {
                    cachedEndpoint = lanFound
                    return@withContext lanFound
                }
            }
        }

        return@withContext null
    }

    private suspend fun checkEndpoints(urls: List<String>, timeoutMs: Long): String? = withContext(Dispatchers.IO) {
        try {
            val deferreds = urls.map { url ->
                async {
                    try {
                        val request = Request.Builder()
                            .url(url) // 仅检测根路径或 /asr
                            .head() // 使用 HEAD 请求减少流量
                            .build()
                        
                        // 使用较短的超时
                        val client = httpClient.newBuilder()
                            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                            .build()

                        withTimeoutOrNull(timeoutMs + 100) {
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful || response.code == 405 || response.code == 404) {
                                    // 405/404 也说明服务存在
                                    url
                                } else {
                                    null
                                }
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            // 等待所有结果，返回第一个非空的
            deferreds.awaitAll().firstOrNull { it != null }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun transcribeOffline(file: File): Result<String> = withContext(Dispatchers.IO) {
        if (sherpaAsrManager == null) {
            return@withContext Result.failure(IllegalStateException("SherpaAsrManager 未初始化"))
        }
        if (!file.exists() || file.length() <= 0) {
            return@withContext Result.failure(IllegalStateException("本地文件不存在或为空"))
        }
        try {
            val text = sherpaAsrManager.transcribe(file)
            if (text.isBlank()) {
                Result.failure(IllegalStateException("SherpaASR 转写结果为空"))
            } else {
                Result.success(text)
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun uploadToTemporaryStorage(apiKey: String, file: File): Result<String> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) {
            return@withContext Result.failure(IllegalStateException("本地文件不存在或为空"))
        }
        val policyResult = requestUploadPolicy(apiKey, "paraformer-v2")
        val policy = policyResult.getOrElse { return@withContext Result.failure(it) }
        if (policy.host.isBlank() || policy.policy.isBlank() || policy.accessId.isBlank() || policy.signature.isBlank()) {
            return@withContext Result.failure(IllegalStateException("上传凭证不完整"))
        }

        val keyTemplate = resolveKeyTemplate(policy.extraFields)
        val objectKey = resolveObjectKey(keyTemplate, policy.dir, file.name)
        val contentType = guessMediaType(file).toMediaTypeOrNull()
        val requestBody = file.asRequestBody(contentType)
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", objectKey)
            .addFormDataPart("policy", policy.policy)
            .addFormDataPart("OSSAccessKeyId", policy.accessId)
            .addFormDataPart("Signature", policy.signature)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("x-oss-object-acl", "private")
            .addFormDataPart("x-oss-forbid-overwrite", "true")
            .apply {
                if (policy.securityToken.isNotBlank()) {
                    addFormDataPart("x-oss-security-token", policy.securityToken)
                }
                policy.extraFields.forEach { (key, value) ->
                    if (value.isNotBlank() && key !in reservedUploadFields) {
                        addFormDataPart(key, value)
                    }
                }
            }
            .addFormDataPart("file", file.name, requestBody)
            .build()

        val uploadHost = normalizeHost(policy.host)
        val uploadRequest = Request.Builder()
            .url(uploadHost)
            .post(multipart)
            .build()

        return@withContext try {
            httpClient.newCall(uploadRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IllegalStateException("上传失败：${response.code} ${body.take(500)}"))
                } else {
                    val ossUrl = buildOssUrl(policy, objectKey)
                    if (ossUrl.isBlank()) {
                        Result.failure(IllegalStateException("无法生成 OSS 地址"))
                    } else {
                        Result.success(ossUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun transcribe(apiKey: String, fileUrl: String): Result<String> = withContext(Dispatchers.IO) {
        val service = RetrofitClient.create(apiKey, ASR_BASE_URL).create(BailianAsrService::class.java)
        return@withContext try {
            val submit = service.submitTranscription(
                AsrRequest(
                    model = "paraformer-v2",
                    input = AsrInput(file_urls = listOf(fileUrl)),
                    parameters = AsrParameters(language_hints = listOf("zh", "en"))
                )
            )
            val taskId = submit.output?.task_id.orEmpty()
            if (taskId.isBlank()) {
                return@withContext Result.failure(IllegalStateException(submit.message ?: "Transcription task id missing"))
            }

            val transcriptionUrlResult = pollTranscriptionUrl(service, taskId)
            val transcriptionUrl = transcriptionUrlResult.getOrElse {
                return@withContext Result.failure(it)
            }

            val transcript = fetchTranscriptText(transcriptionUrl)
            if (transcript.isBlank()) {
                Result.failure(IllegalStateException("Transcription text empty"))
            } else {
                Result.success(transcript)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarize(apiKey: String, transcript: String, modelName: String = "qwen-turbo", baseUrl: String = "https://dashscope.aliyuncs.com/compatible-mode/v1/"): Result<String> = withContext(Dispatchers.IO) {
        val service = RetrofitClient.create(apiKey, baseUrl).create(BailianChatService::class.java)
        return@withContext try {
            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = "你是学习助手，请基于给定转写内容生成中文要点摘要。"
                    ),
                    ChatMessage(
                        role = "user",
                        content = "请对以下视频转写内容生成结构化摘要：\n$transcript"
                    )
                ),
                parameters = ChatParameters()
            )
            val response = service.chat(request)
            val content = extractSummary(response)
            if (content.isBlank()) {
                Result.failure(IllegalStateException("Summary empty"))
            } else {
                Result.success(content)
            }
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401) {
                Result.failure(IllegalStateException("API Key 无效或未授权，请在设置中检查您的 API Key。"))
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun pollTranscriptionUrl(service: BailianAsrService, taskId: String): Result<String> {
        repeat(60) {
            val task = service.getTask(taskId)
            val status = task.output?.task_status.orEmpty()
            if (status.equals("SUCCEEDED", true)) {
                val url = task.output?.results?.firstOrNull()?.transcription_url.orEmpty()
                if (url.isNotBlank()) {
                    return Result.success(url)
                }
                return Result.failure(IllegalStateException("Task succeeded but URL is empty"))
            }
            if (status.equals("FAILED", true)) {
                val errorMsg = task.message ?: task.code ?: "Unknown error"
                return Result.failure(IllegalStateException("Task failed: $errorMsg"))
            }
            delay(2000)
        }
        return Result.failure(IllegalStateException("Task timed out after 120 seconds"))
    }

    private fun fetchTranscriptText(transcriptionUrl: String): String {
        val request = Request.Builder().url(transcriptionUrl).get().build()
        val response = httpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) return ""
            val body = it.body?.string().orEmpty()
            if (body.isBlank()) return ""
            return parseTranscript(body)
        }
    }

    private fun parseTranscript(rawJson: String): String {
        val json = gson.fromJson(rawJson, JsonObject::class.java)
        val transcripts = json.getAsJsonArray("transcripts") ?: return ""
        val parts = mutableListOf<String>()
        transcripts.forEach { item ->
            val text = item.asJsonObject?.get("text")?.asString?.trim().orEmpty()
            if (text.isNotBlank()) parts.add(text)
        }
        return parts.joinToString(" ")
    }

    private fun extractSummary(response: ChatResponse): String {
        val choice = response.choices?.firstOrNull()?.message?.content
            ?: response.output?.choices?.firstOrNull()?.message?.content
            ?: response.output?.text
        return when (choice) {
            is String -> choice
            null -> ""
            else -> choice.toString()
        }.trim()
    }

    private data class UploadPolicy(
        val host: String,
        val dir: String,
        val policy: String,
        val accessId: String,
        val signature: String,
        val securityToken: String,
        val ossUrl: String,
        val bucket: String,
        val extraFields: Map<String, String>
    )

    private fun requestUploadPolicy(apiKey: String, model: String): Result<UploadPolicy> {
        val url = "${ASR_BASE_URL}uploads?action=getPolicy&model=$model"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $apiKey")
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("获取上传凭证失败：${response.code}"))
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return Result.failure(IllegalStateException("上传凭证为空"))
                val json = gson.fromJson(body, JsonObject::class.java)
                val data = json.getAsJsonObject("data") ?: json
                val extraFields = extractExtraFields(data)
                val securityToken = extractString(data, "security_token", "securityToken").ifBlank {
                    extraFields["x-oss-security-token"].orEmpty()
                }
                val policy = UploadPolicy(
                    host = extractString(data, "host", "upload_host"),
                    dir = extractString(data, "dir", "directory"),
                    policy = extractString(data, "policy"),
                    accessId = extractString(data, "access_id", "accessid", "oss_access_key_id", "accessKeyId", "ossAccessKeyId"),
                    signature = extractString(data, "signature"),
                    securityToken = securityToken,
                    ossUrl = extractString(data, "oss_url", "ossUrl", "url"),
                    bucket = extractString(data, "bucket", "bucket_name"),
                    extraFields = extraFields
                )
                Result.success(policy)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val reservedUploadFields = setOf(
        "key",
        "policy",
        "OSSAccessKeyId",
        "Signature",
        "success_action_status",
        "x-oss-security-token",
        "x-oss-object-acl",
        "x-oss-forbid-overwrite"
    )

    private val reservedPolicyKeys = setOf(
        "host",
        "upload_host",
        "dir",
        "directory",
        "policy",
        "access_id",
        "accessid",
        "oss_access_key_id",
        "accessKeyId",
        "ossAccessKeyId",
        "signature",
        "security_token",
        "securityToken",
        "oss_url",
        "ossUrl",
        "url",
        "bucket",
        "bucket_name"
    )

    private fun extractExtraFields(data: JsonObject?): Map<String, String> {
        if (data == null) return emptyMap()
        val fields = linkedMapOf<String, String>()
        val fieldContainers = listOf("fields", "form", "form_fields")
        fieldContainers.forEach { key ->
            val obj = data.getAsJsonObject(key)
            if (obj != null) {
                obj.entrySet().forEach { entry ->
                    val value = entry.value
                    if (value != null && value.isJsonPrimitive) {
                        val text = value.asString
                        if (text.isNotBlank()) fields[entry.key] = text
                    }
                }
            }
        }
        data.entrySet().forEach { entry ->
            if (entry.key in reservedPolicyKeys) return@forEach
            val value = entry.value
            if (value != null && value.isJsonPrimitive) {
                val text = value.asString
                if (text.isNotBlank()) fields[entry.key] = text
            }
        }
        return fields
    }

    private fun extractString(obj: JsonObject?, vararg keys: String): String {
        if (obj == null) return ""
        for (key in keys) {
            if (obj.has(key)) {
                val value = obj.get(key)
                if (value != null && !value.isJsonNull) {
                    val text = value.asString
                    if (text.isNotBlank()) return text
                }
            }
        }
        return ""
    }

    private fun buildObjectKey(dir: String, fileName: String): String {
        val safeDir = dir.trim()
        return when {
            safeDir.isBlank() -> fileName
            safeDir.endsWith("/") -> safeDir + fileName
            else -> "$safeDir/$fileName"
        }
    }

    private fun resolveObjectKey(template: String, dir: String, fileName: String): String {
        if (template.isBlank()) return buildObjectKey(dir, fileName)
        val replaced = template
            .replace("\${filename}", fileName)
            .replace("\${fileName}", fileName)
        return when {
            replaced.endsWith("/") -> replaced + fileName
            else -> replaced
        }
    }

    private fun resolveKeyTemplate(extraFields: Map<String, String>): String {
        return extraFields["key"]
            ?: extraFields["object_key"]
            ?: extraFields["objectKey"]
            ?: extraFields["prefix"]
            ?: extraFields["upload_dir"]
            ?: ""
    }

    private fun buildOssUrl(policy: UploadPolicy, objectKey: String): String {
        val base = policy.ossUrl.trim()
        if (base.isNotBlank()) {
            return when {
                base.contains("{object}") -> base.replace("{object}", objectKey)
                base.endsWith("/") -> base + objectKey
                base.startsWith("oss://") && !base.contains("/") -> "$base/$objectKey"
                base.startsWith("oss://") -> base
                else -> base
            }
        }
        val bucket = policy.bucket.ifBlank { parseBucketFromHost(policy.host) }
        if (bucket.isBlank()) return ""
        return "oss://$bucket/$objectKey"
    }

    private fun parseBucketFromHost(host: String): String {
        return try {
            val uri = URI(normalizeHost(host))
            val hostName = uri.host?.lowercase().orEmpty()
            if (hostName.isBlank()) return ""
            val index = hostName.indexOf(".oss-")
            if (index > 0) hostName.substring(0, index) else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun normalizeHost(host: String): String {
        val trimmed = host.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isNotBlank() -> "https://$trimmed"
            else -> ""
        }
    }



    private fun guessMediaType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            else -> "application/octet-stream"
        }
    }
}
