package com.example.summarizer.video_summarizer.services

import android.content.Context
import android.util.Log
import com.example.common.dispatchers.DispatcherProvider
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

class ModelDownloader(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider
) {

    companion object {
        private const val TAG = "ModelDownloader"
        const val MODEL_DIR_NAME = "sherpa-onnx-paraformer-zh-2023-09-14"
        private const val BASE_MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-paraformer-zh-2023-09-14.tar.bz2"
        val MODEL_URLS = listOf(
            "https://ghproxy.net/$BASE_MODEL_URL",
            "https://mirror.ghproxy.com/$BASE_MODEL_URL",
            "https://gh-proxy.com/$BASE_MODEL_URL",
            BASE_MODEL_URL
        )
    }

    fun isModelReady(): Boolean {
        val modelDir = File(context.externalCacheDir ?: context.cacheDir, MODEL_DIR_NAME)
        // Ensure that key files are present to consider the model ready
        val onnxFile = File(modelDir, "model.int8.onnx")
        val tokensFile = File(modelDir, "tokens.txt")
        return modelDir.exists() && modelDir.isDirectory && onnxFile.exists() && tokensFile.exists()
    }

    suspend fun downloadAndExtractModel(onProgress: (DownloadProgress) -> Unit): Result<File> = withContext(dispatcherProvider.io) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val tarBz2File = File(cacheDir, "sherpa-model.tar.bz2")
        val destDir = cacheDir
        
        try {
            // 1. Download
            if (!tarBz2File.exists() || tarBz2File.length() < 1024 * 1024) { // rudimentary check
                onProgress(DownloadProgress(status = DownloadStatus.PREPARING))
                
                var downloadSuccess = false
                var lastException: Exception? = null

                for (urlStr in MODEL_URLS) {
                    try {
                        Log.d(TAG, "Attempting to download from: $urlStr")
                        val connection = URL(urlStr).openConnection() as HttpURLConnection
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.connect()

                        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                            throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                        }

                        val fileLength = connection.contentLengthLong

                        connection.inputStream.use { input ->
                            FileOutputStream(tarBz2File).use { output ->
                                val data = ByteArray(8192)
                                var total: Long = 0
                                var count: Int
                                var lastProgress = 0f
                                
                                onProgress(DownloadProgress(status = DownloadStatus.DOWNLOADING, progress = 0f, downloadedBytes = 0, totalBytes = fileLength))

                                while (input.read(data).also { count = it } != -1) {
                                    total += count
                                    output.write(data, 0, count)
                                    
                                    if (fileLength > 0) {
                                        val progress = (total * 100f / fileLength)
                                        if (progress - lastProgress >= 1f) {
                                            lastProgress = progress
                                            onProgress(DownloadProgress(
                                                status = DownloadStatus.DOWNLOADING,
                                                progress = progress,
                                                downloadedBytes = total,
                                                totalBytes = fileLength
                                            ))
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (tarBz2File.exists() && tarBz2File.length() > 1024 * 1024) {
                            downloadSuccess = true
                            Log.d(TAG, "Successfully downloaded from: $urlStr")
                            break
                        } else {
                            throw Exception("Downloaded file is too small or does not exist")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to download from $urlStr", e)
                        lastException = e
                        if (tarBz2File.exists()) {
                            tarBz2File.delete()
                        }
                    }
                }

                if (!downloadSuccess) {
                    throw Exception("Failed to download model from all available mirrors", lastException)
                }
            }

            // 2. Extract
            onProgress(DownloadProgress(status = DownloadStatus.PREPARING)) // Using PREPARING to indicate extracting
            Log.d(TAG, "Starting extraction of ${tarBz2File.absolutePath} to ${destDir.absolutePath}")
            
            FileInputStream(tarBz2File).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    BZip2CompressorInputStream(bis).use { bzIn ->
                        TarArchiveInputStream(bzIn).use { tarIn ->
                            var entry = tarIn.nextTarEntry
                            while (entry != null) {
                                val destFile = File(destDir, entry.name)
                                if (entry.isDirectory) {
                                    destFile.mkdirs()
                                } else {
                                    destFile.parentFile?.mkdirs()
                                    FileOutputStream(destFile).use { fos ->
                                        tarIn.copyTo(fos)
                                    }
                                }
                                entry = tarIn.nextTarEntry
                            }
                        }
                    }
                }
            }

            // Clean up tar.bz2
            tarBz2File.delete()

            val extractedModelDir = File(destDir, MODEL_DIR_NAME)
            onProgress(DownloadProgress(status = DownloadStatus.COMPLETED, progress = 100f))
            Result.success(extractedModelDir)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and extract model", e)
            tarBz2File.delete()
            onProgress(DownloadProgress(status = DownloadStatus.FAILED))
            Result.failure(e)
        }
    }
}
