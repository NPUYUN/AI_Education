package com.example.video_summarizer.data.downloader

import android.content.Context
import android.os.Environment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.URL

data class VideoInfo(
    val url: String,
    val title: String = "",
    val thumbnail: String = "",
    val duration: String = "",
    val formats: List<VideoFormat> = emptyList()
)

data class VideoFormat(
    val formatId: String,
    val resolution: String,
    val extension: String,
    val size: String
)

data class DownloadProgress(
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val currentSpeed: String = "",
    val eta: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L
)

enum class DownloadStatus {
    IDLE,
    PREPARING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

class VideoDownloader(private val context: Context) {

    private var isCancelled = false
    private var activeProcessId: String? = null
    private var isInitialized = false

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        try {
            YoutubeDL.getInstance().init(context)
            /*
            try {
                FFmpeg.getInstance().init(context)
            } catch (e: Exception) {
                // Ignore if FFmpeg init fails
                e.printStackTrace()
            }
            */
            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    context,
                    YoutubeDL.UpdateChannel.STABLE
                )
            } catch (_: Exception) {
            }
            isInitialized = true
        } catch (e: Exception) {
            isInitialized = false
            throw e
        }
    }

    suspend fun getVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        getVideoInfoInternal(url, allowUpdate = true)
    }

    suspend fun downloadVideo(
        url: String,
        onProgress: (DownloadProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        downloadVideoInternal(url, onProgress, allowUpdate = true)
    }

    suspend fun getAudioUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            ensureInitialized()
            val resolvedUrl = resolveRedirects(url)
            val request = YoutubeDLRequest(resolvedUrl)
            request.addOption("-f", "bestaudio")
            request.addOption("-g")
            request.addOption("--no-playlist")
            val response = YoutubeDL.getInstance().execute(request)
            val directUrl = response.out
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
        isCancelled = true
        activeProcessId?.let { processId ->
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveRedirects(inputUrl: String): String {
        return try {
            var current = inputUrl.trim()
            if (!current.startsWith("http://") && !current.startsWith("https://")) return current

            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            })
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
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                    )
                    connection.setRequestProperty("Accept", "*/*")
                    connection.setRequestProperty("Range", "bytes=0-0")

                    connection.connect()
                    val code = connection.responseCode
                    if (code in 300..399) {
                        val location = connection.getHeaderField("Location")?.trim().orEmpty()
                        if (location.isBlank()) break
                        val next = try {
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
        block: suspend (resolvedUrl: String) -> Result<T>
    ): Result<T>? {
        if (!shouldAttemptUpdate(error)) return null

        return try {
            val resolved = resolveRedirects(originalUrl)

            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    context,
                    YoutubeDL.UpdateChannel.STABLE
                )
            } catch (_: Exception) {
            }

            block(resolved)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getVideoInfoInternal(url: String, allowUpdate: Boolean): Result<VideoInfo> {
        return try {
            ensureInitialized()
            val resolvedUrl = resolveRedirects(url)
            val info = YoutubeDL.getInstance().getInfo(resolvedUrl)
            val videoInfo = VideoInfo(
                url = resolvedUrl,
                title = info.title ?: "",
                thumbnail = info.thumbnail ?: "",
                duration = info.duration.toString(),
                formats = info.formats?.map {
                    VideoFormat(
                        formatId = it.formatId ?: "",
                        resolution = "${it.width}x${it.height}",
                        extension = it.ext ?: "",
                        size = it.fileSize.toString()
                    )
                } ?: emptyList()
            )
            Result.success(videoInfo)
        } catch (e: Exception) {
            if (!allowUpdate) return Result.failure(e)
            val retried = retryUpdateIfNeeded(e, url) { resolved ->
                getVideoInfoInternal(resolved, allowUpdate = false)
            }
            retried ?: Result.failure(e)
        }
    }

    private suspend fun downloadVideoInternal(
        url: String,
        onProgress: (DownloadProgress) -> Unit,
        allowUpdate: Boolean
    ): Result<String> {
        return try {
            ensureInitialized()
            isCancelled = false
            onProgress(DownloadProgress(status = DownloadStatus.PREPARING))

            val resolvedUrl = resolveRedirects(url)
            val downloadDir = getDownloadDirectory()
            val timestamp = System.currentTimeMillis()
            val fileName = "video_$timestamp.%(ext)s"
            val request = YoutubeDLRequest(resolvedUrl)
            request.addOption("-o", File(downloadDir, fileName).absolutePath)
            request.addOption("--no-mtime")
            request.addOption("--no-playlist")
            request.addOption(
                "-f",
                "bestvideo+bestaudio/best"
            )
            request.addOption("--merge-output-format", "mp4")
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            request.addOption("--socket-timeout", "30")
            request.addOption("--no-check-certificate")
            request.addOption("--force-ipv4")
            if (resolvedUrl.contains("bilibili.com") || resolvedUrl.contains("b23.tv")) {
                request.addOption("--referer", "https://www.bilibili.com")
                request.addOption("--extractor-args", "bilibili:api=app")
            }

            val processId = "video_download_$timestamp"
            activeProcessId = processId

            val response = YoutubeDL.getInstance().execute(
                request,
                processId = processId,
                callback = { progress: Float, etaInSeconds: Long, _: String ->
                    if (isCancelled) {
                        throw InterruptedException("Download cancelled")
                    }
                    onProgress(
                        DownloadProgress(
                            progress = progress,
                            status = DownloadStatus.DOWNLOADING,
                            currentSpeed = "",
                            eta = "${etaInSeconds}s",
                            downloadedBytes = 0,
                            totalBytes = 0
                        )
                    )
                }
            )

            if (isCancelled) {
                onProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
                Result.failure(Exception("Download cancelled"))
            } else {
                // Parse output to find downloaded files
                val logs = response.out
                val destinationRegex = "Destination: (.+)".toRegex()
                val destinations = destinationRegex.findAll(logs).map { it.groupValues[1].trim() }.toList()
                val alreadyDownloadedRegex = "has already been downloaded and merged".toRegex() // Handle cached case
                
                var finalFile: File? = null
                
                // Check for merged file
                val mergedRegex = "Merging formats into \"(.+)\"".toRegex()
                val mergedMatch = mergedRegex.find(logs)
                
                if (mergedMatch != null) {
                    finalFile = File(mergedMatch.groupValues[1])
                } else if (destinations.size >= 2) {
                    // Manual Merge using FFmpegKit
                    val videoFile = File(destinations.firstOrNull { it.endsWith(".mp4") || it.endsWith(".webm") } ?: destinations[0])
                    val audioFile = File(destinations.firstOrNull { it.endsWith(".m4a") || it.endsWith(".webm") && it != videoFile.absolutePath } ?: destinations[1])
                    
                    if (videoFile.exists() && audioFile.exists()) {
                        val outputFile = File(downloadDir, "video_${timestamp}_merged.mp4")
                        val ffmpegCommand = "-i \"${videoFile.absolutePath}\" -i \"${audioFile.absolutePath}\" -c:v copy -c:a aac -strict experimental \"${outputFile.absolutePath}\""
                        
                        val session = FFmpegKit.execute(ffmpegCommand)
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            finalFile = outputFile
                            // Cleanup parts
                            videoFile.delete()
                            audioFile.delete()
                        } else {
                            throw Exception("FFmpeg merge failed: ${session.failStackTrace}")
                        }
                    }
                } else {
                    // Single file download or already merged
                     val downloadedFile = downloadDir.listFiles()?.find {
                        it.name.startsWith("video_$timestamp") && !it.name.endsWith(".part") && !it.name.contains("merged")
                    }
                    finalFile = downloadedFile
                }

                if (finalFile != null && finalFile.exists()) {
                    try {
                        val mediaInfoSession = FFprobeKit.getMediaInformation(finalFile.absolutePath)
                        val mediaInformation = mediaInfoSession.mediaInformation
                        val hasAudio = mediaInformation?.streams?.any { it.type == "audio" } ?: false
                        
                        if (!hasAudio) {
                            finalFile.delete()
                            onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                            Result.failure(IllegalStateException("下载的视频不包含音频流，且自动合并失败，请重试"))
                        } else {
                            onProgress(DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f))
                            Result.success(finalFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                        Result.failure(Exception("验证媒体文件失败: ${e.message}"))
                    }
                } else {
                    onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                    Result.failure(Exception("Downloaded file not found or merge failed"))
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("cancelled") == true || e is InterruptedException) {
                onProgress(DownloadProgress(status = DownloadStatus.CANCELLED))
                Result.failure(Exception("Download cancelled"))
            } else {
                if (!allowUpdate) {
                    onProgress(DownloadProgress(status = DownloadStatus.FAILED))
                    Result.failure(e)
                } else {
                    val retried = retryUpdateIfNeeded(e, url) { resolved ->
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

    private fun getDownloadDirectory(): File {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir.resolve("downloads").also { it.mkdirs() }
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }
}
