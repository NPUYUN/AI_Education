package com.example.ai_tutor.presentation.manager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object VoskModelManager {
    // Use HF Mirror for faster download in China
    private const val MODEL_URL = "https://hf-mirror.com/localstack/vosk-models/resolve/main/vosk-model-small-cn-0.22.zip"
    private const val MODEL_DIR_NAME = "vosk-model-small-cn-0.22"
    
    private var loadedModel: Model? = null
    
    // Scope for background download that survives UI lifecycle
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    sealed class InitState {
        object Idle : InitState()
        data class Downloading(val progress: Float) : InitState()
        object Loading : InitState()
        object Ready : InitState()
        data class Error(val message: String) : InitState()
    }

    private val _initState = MutableStateFlow<InitState>(InitState.Idle)
    val initState = _initState.asStateFlow()

    fun initModel(context: Context) {
        if (_initState.value is InitState.Ready || 
            _initState.value is InitState.Loading || 
            _initState.value is InitState.Downloading) {
            return
        }

        scope.launch {
            try {
                if (loadedModel != null) {
                    _initState.value = InitState.Ready
                    return@launch
                }

                val modelDir = File(context.getExternalFilesDir(null), MODEL_DIR_NAME)
                val successMarker = File(modelDir, "_SUCCESS")

                if (!modelDir.exists() || !successMarker.exists()) {
                     // Cleanup
                    if (modelDir.exists()) modelDir.deleteRecursively()

                    _initState.value = InitState.Downloading(0f)
                    
                    downloadAndUnzipModel(context, modelDir.parentFile!!) { progress ->
                        _initState.value = InitState.Downloading(progress)
                    }
                    
                    if (modelDir.exists()) successMarker.createNewFile()
                }

                _initState.value = InitState.Loading
                LibVosk.setLogLevel(LogLevel.INFO)
                loadedModel = Model(modelDir.absolutePath)
                _initState.value = InitState.Ready
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Cleanup on error
                val modelDir = File(context.getExternalFilesDir(null), MODEL_DIR_NAME)
                 if (modelDir.exists()) modelDir.deleteRecursively()
                _initState.value = InitState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    // Legacy support / Direct access
    fun getModel(context: Context, onProgress: ((Float, String) -> Unit)? = null): Model? {
        if (loadedModel != null) return loadedModel
        
        // If init hasn't started, start it
        initModel(context)
        
        // Return current state (likely null if just started)
        return loadedModel
    }

    private fun downloadAndUnzipModel(context: Context, destDir: File, onDownloadProgress: (Float) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(MODEL_URL).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download model: $response")

            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            val source = body.source()
            
            val zipFile = File(destDir, "model.zip")
            val fos = FileOutputStream(zipFile)
            
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalBytesRead: Long = 0
            
            while (source.read(buffer).also { bytesRead = it } != -1) {
                fos.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    onDownloadProgress(totalBytesRead.toFloat() / contentLength)
                }
            }
            fos.close()

            // Unzip
            unzip(zipFile, destDir)
            zipFile.delete()
        }
    }
    
    private fun unzip(zipFile: File, targetDirectory: File) {
        val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile))
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            val file = File(targetDirectory, zipEntry.name)
            if (zipEntry.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                val fileOutputStream = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var count: Int
                while (zipInputStream.read(buffer).also { count = it } != -1) {
                    fileOutputStream.write(buffer, 0, count)
                }
                fileOutputStream.close()
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }
}
