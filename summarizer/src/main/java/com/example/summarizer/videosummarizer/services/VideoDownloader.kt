package com.example.summarizer.videosummarizer.services

import android.content.Context
import android.os.Environment
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.common.dispatchers.DispatcherProvider
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class VideoInfo(
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val formats: List<VideoFormat> = emptyList(),
)

data class VideoFormat(
    val formatId: String,
    val resolution: String,
    val extension: String,
    val size: String,
)

data class DownloadProgress(
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val currentSpeed: String = "",
    val eta: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
)

enum class DownloadStatus {
    IDLE,
    PREPARING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

class VideoDownloader(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val isCancelled = AtomicBoolean(false)
    private var activeProcessId: String? = null
    private var isInitialized = false

    private suspend fun ensureInitialized() =
        withContext(dispatcherProvider.io) {
            if (isInitialized) return@withContext
            try {
                tryInitYoutubeDl()
            } catch (e: Exception) {
                if (isNativeAbiMismatchError(e)) {
                    clearYoutubeDlExtractedFiles()
                    tryInitYoutubeDl()
                } else {
                    isInitialized = false
                    throw e
                }
            }
        }

    private fun tryInitYoutubeDl() {
        YoutubeDL.getInstance().init(context)
        try {
            YoutubeDL.getInstance().updateYoutubeDL(
                context,
                YoutubeDL.UpdateChannel.STABLE,
            )
        } catch (_: Exception) {
        }
        isInitialized = true
    }

    private fun isNativeAbiMismatchError(e: Throwable): Boolean {
        val msg =
            buildString {
                append(e.message.orEmpty())
                e.cause?.message?.let { append(it) }
            }.lowercase()
        return msg.contains("cannot link executable") ||
            msg.contains("em_aarch64") ||
            msg.contains("em_x86_64") ||
            msg.contains("wrong elf class") ||
            (msg.contains("libz.so") && (msg.contains("instead of") || msg.contains("mismatch")))
    }

    private fun clearYoutubeDlExtractedFiles() {
        isInitialized = false
        try {
            val base = context.noBackupFilesDir ?: return
            File(base, "youtubedl-android").takeIf { it.exists() }?.deleteRecursively()
        } catch (_: Exception) {
        }
    }

    suspend fun getVideoInfo(url: String): Result<VideoInfo> =
        withContext(dispatcherProvider.io) {
            getVideoInfoInternal(url, allowUpdate = true)
        }

    suspend fun downloadVideo(
        url: String,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<String> =
        withContext(dispatcherProvider.io) {
            downloadVideoInternal(url, onProgress, allowUpdate = true)
        }

    suspend fun getAudioUrl(url: String): Result<String> =
        withContext(dispatcherProvider.io) {
            return@withContext try {
                ensureInitialized()
                val resolvedUrl = resolveRedirects(url)
                val request = YoutubeDLRequest(resolvedUrl)
                request.addOption("-f", "bestaudio")
                request.addOption("-g")
                request.addOption("--no-playlist")
                val response = YoutubeDL.getInstance().execute(request)
                val directUrl =
                    response.out
                        .lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("http") }
                        .orEmpty()

                if (directUrl.isBlank()) {
                    Result.failure(IllegalStateException("Audio url not found"))
                } else {
                    Result.success(directUrl)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun cancelDownload() {
        isCancelled.set(true)
        activeProcessId?.let { processId ->
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 移动版 B 站域名在部分 yt-dlp 版本上解析不稳定（易导致下载/合并失败甚至 native 异常），统一为桌面站。
     * 例：https://m.bilibili.com/video/BV1kv411574Y → https://www.bilibili.com/video/BV1kv411574Y
     */
    private fun normalizeExtractorUrl(url: String): String {
        var u = url.trim()
        if (u.contains("bilibili.com", ignoreCase = true)) {
            u =
                u.replace("://m.bilibili.com", "://www.bilibili.com", ignoreCase = true)
        }
        return u
    }

    private fun resolveRedirects(inputUrl: String): String {
        return try {
            var current = normalizeExtractorUrl(inputUrl)
            if (!current.startsWith("http://") && !current.startsWith("https://")) return current

            val trustAllCerts =
                arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null

                        override fun checkClientTrusted(
                            certs: Array<java.security.cert.X509Certificate>,
                            authType: String,
                        ) {}

                        override fun checkServerTrusted(
                            certs: Array<java.security.cert.X509Certificate>,
                            authType: String,
                        ) {}
                    },
                )
            val sc = javax.net.ssl.SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            val socketFactory = sc.socketFactory
            val hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }

            var hops = 0
            while (hops < 8) {
                val connection = (URL(current).openConnection() as java.net.HttpURLConnection)
                if (connection is javax.net.ssl.HttpsURLConnection) {
                    connection.sslSocketFactory = socketFactory
                    connection.hostnameVerifier = hostnameVerifier
                }

                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                    )
                    connection.setRequestProperty("Accept", "*/*")
                    connection.setRequestProperty("Range", "bytes=0-0")

                    connection.connect()
                    val code = connection.responseCode
                    if (code in 300..399) {
                        val location = connection.getHeaderField("Location")?.trim().orEmpty()
                        if (location.isBlank()) break
                        val next =
                            try {
                                URI(current).resolve(location).toString()
                            } catch (_: Exception) {
                                location
                            }
                        current = next
                        hops++
                        continue
                    }
                    break
                } finally {
                    try {
                        connection.disconnect()
                    } catch (_: Exception) {
                    }
                }
            }
            current
        } catch (_: Exception) {
            inputUrl
        }
    }

    private fun shouldAttemptUpdate(error: Throwable): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return (message.contains("nonetype") && message.contains("lower")) ||
            message.contains("no suitable extractor") ||
            message.contains("unsupported url") ||
            message.contains("signature extraction") ||
            message.contains("unable to extract") ||
            message.contains("this site is unsupported")
    }

    private suspend fun <T> retryUpdateIfNeeded(
        error: Throwable,
        originalUrl: String,
        block: suspend (resolvedUrl: String) -> Result<T>,
    ): Result<T>? {
        if (!shouldAttemptUpdate(error)) return null

        return try {
            val resolved = resolveRedirects(originalUrl)

            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    context,
                    YoutubeDL.UpdateChannel.STABLE,
                )
            } catch (_: Exception) {
            }

            block(resolved)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getVideoInfoInternal(
        url: String,
        allowUpdate: Boolean,
    ): Result<VideoInfo> {
        return try {
            ensureInitialized()
            val resolvedUrl = resolveRedirects(url)
            val info = YoutubeDL.getInstance().getInfo(resolvedUrl)
            val videoInfo =
                VideoInfo(
                    url = resolvedUrl,
                    title = info.title ?: "",
                    thumbnail = info.thumbnail ?: "",
                    duration = info.duration.toString(),
                    formats =
                        info.formats?.map {
                            VideoFormat(
                                formatId = it.formatId ?: "",
                                resolution = "${it.width}x${it.height}",
                                extension = it.ext ?: "",
                                size = it.fileSize.toString(),
                            )
                        } ?: emptyList(),
                )
            Result.success(videoInfo)
        } catch (e: Exception) {
            if (!allowUpdate) return Result.failure(e)
            val retried =
                retryUpdateIfNeeded(e, url) { resolved ->
                    getVideoInfoInternal(resolved, allowUpdate = false)
                }
            retried ?: Result.failure(e)
        }
    }

    private suspend fun downloadVideoInternal(
        url: String,
        onProgress: (DownloadProgress) -> Unit,
        allowUpdate: Boolean,
    ): Result<String> {
        return try {
            ensureInitialized()
            isCancelled.set(false)
            onProgress(DownloadProgress(status = DownloadStatus.PREPARING))

            val resolvedUrl = resolveRedirects(url)
            val downloadDir = getDownloadDirectory()
            val timestamp = System.currentTimeMillis()
            val formatStrategies =
                listOf(
                    "bestvideo+bestaudio/best",
                    "best[ext=mp4]/best",
                    "best",
                )

            var lastError: Exception? = null
            for ((idx, format) in formatStrategies.withIndex()) {
                if (isCancelled.get()) {
                    onProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
                    return Result.failure(Exception("Download cancelled"))
                }

                val attemptTimestamp = timestamp + idx
                val attemptFilePrefix = "video_$attemptTimestamp"
                val request = YoutubeDLRequest(resolvedUrl)
                request.addOption("-o", File(downloadDir, "$attemptFilePrefix.%(ext)s").absolutePath)
                request.addOption("--no-mtime")
                request.addOption("--no-playlist")
                request.addOption("-f", format)
                request.addOption("--merge-output-format", "mp4")
                request.addOption(
                    "--user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                )
                request.addOption("--socket-timeout", "30")
                request.addOption("--no-check-certificate")
                request.addOption("--force-ipv4")
                if (resolvedUrl.contains("bilibili.com") || resolvedUrl.contains("b23.tv")) {
                    request.addOption("--referer", "https://www.bilibili.com")
                    if (idx == 0) {
                        request.addOption("--extractor-args", "bilibili:api=app")
                    }
                }

                val processId = "video_download_$attemptTimestamp"
                activeProcessId = processId

                try {
                    val response =
                        YoutubeDL.getInstance().execute(
                            request,
                            processId = processId,
                            callback = { progress: Float, etaInSeconds: Long, _: String ->
                                if (!isCancelled.get()) {
                                    onProgress(
                                        DownloadProgress(
                                            progress = progress,
                                            status = DownloadStatus.DOWNLOADING,
                                            currentSpeed = "",
                                            eta = "${etaInSeconds}s",
                                            downloadedBytes = 0,
                                            totalBytes = 0,
                                        ),
                                    )
                                }
                            },
                        )

                    if (isCancelled.get()) {
                        onProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
                        return Result.failure(Exception("Download cancelled"))
                    }

                    val finalFile = resolveDownloadedFile(downloadDir, attemptTimestamp, response.out)
                    if (finalFile != null && finalFile.exists()) {
                        val mediaInfoSession = FFprobeKit.getMediaInformation(finalFile.absolutePath)
                        val mediaInformation = mediaInfoSession.mediaInformation
                        val hasAudio = mediaInformation?.streams?.any { it.type == "audio" } ?: false
                        if (!hasAudio) {
                            finalFile.delete()
                            lastError = IllegalStateException("下载的视频不包含音频流")
                            continue
                        }
                        onProgress(DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f))
                        return Result.success(finalFile.absolutePath)
                    } else {
                        lastError = Exception("Downloaded file not found or merge failed")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            onProgress(DownloadProgress(status = DownloadStatus.FAILED))
            Result.failure(lastError ?: Exception("下载失败"))
        } catch (e: Exception) {
            if (e.message?.contains("cancelled") == true || e is InterruptedException) {
                onProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
                Result.failure(Exception("Download cancelled"))
            } else {
                if (!allowUpdate) {
                    onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                    Result.failure(e)
                } else {
                    val retried =
                        retryUpdateIfNeeded(e, url) { resolved ->
                            downloadVideoInternal(resolved, onProgress, allowUpdate = false)
                        }
                    retried ?: run {
                        onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                        Result.failure(e)
                    }
                }
            }
        } finally {
            activeProcessId = null
        }
    }

    private fun resolveDownloadedFile(
        downloadDir: File,
        timestamp: Long,
        logs: String,
    ): File? {
        val destinationRegex = "Destination: (.+)".toRegex()
        val destinations = destinationRegex.findAll(logs).map { it.groupValues[1].trim() }.toList()

        val mergedRegex = "Merging formats into \"(.+)\"".toRegex()
        val mergedMatch = mergedRegex.find(logs)
        if (mergedMatch != null) {
            return File(mergedMatch.groupValues[1])
        }

        if (destinations.size >= 2) {
            val videoFile = File(destinations.firstOrNull { it.endsWith(".mp4") || it.endsWith(".webm") } ?: destinations[0])
            val audioFile =
                File(
                    destinations.firstOrNull {
                        it.endsWith(".m4a") || (it.endsWith(".webm") && it != videoFile.absolutePath)
                    } ?: destinations[1],
                )
            if (videoFile.exists() && audioFile.exists()) {
                val outputFile = File(downloadDir, "video_${timestamp}_merged.mp4")
                val ffmpegCommand =
                    "-i \"${videoFile.absolutePath}\" " +
                        "-i \"${audioFile.absolutePath}\" -c:v copy -c:a aac -strict experimental " +
                        "\"${outputFile.absolutePath}\""
                val session = FFmpegKit.execute(ffmpegCommand)
                if (ReturnCode.isSuccess(session.returnCode)) {
                    videoFile.delete()
                    audioFile.delete()
                    return outputFile
                }
            }
        }

        return downloadDir.listFiles()?.find {
            it.name.startsWith("video_$timestamp") &&
                !it.name.endsWith(".part") &&
                !it.name.contains("merged")
        }
    }

    private fun getDownloadDirectory(): File {
        val downloadsDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir.resolve("downloads").also { it.mkdirs() }
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }
}
