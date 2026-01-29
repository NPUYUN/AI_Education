package com.example.video_summarizer.data.asr

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.WaveReader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SherpaAsrManager(private val context: Context) {
    private var recognizer: OfflineRecognizer? = null
    private val TAG = "SherpaAsrManager"
    private val mutex = Mutex()

    // Initialize the recognizer lazily (Must be called within mutex lock)
    private fun getRecognizer(): OfflineRecognizer {
        if (recognizer == null) {
            try {
                // Initialize config
                // The model files are located in assets/sherpa-onnx-paraformer-zh-2023-09-14
                val modelDir = "sherpa-onnx-paraformer-zh-2023-09-14"
                val config = OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        paraformer = OfflineParaformerModelConfig(
                            model = "$modelDir/model.int8.onnx"
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "paraformer",
                        debug = true
                    )
                )
                
                recognizer = OfflineRecognizer(
                    assetManager = context.assets,
                    config = config
                )
                Log.d(TAG, "Sherpa-onnx initialized successfully")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize Sherpa-onnx", e)
                throw IllegalStateException("Failed to initialize speech recognizer: ${e.message}", e)
            }
        }
        return recognizer!!
    }

    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        var wavFile: File? = null
        var stream: OfflineStream? = null
        try {
            Log.d(TAG, "Starting transcription for: ${audioFile.name}")
            // 0. Check if file has audio stream
            try {
                val mediaInfoSession = FFprobeKit.getMediaInformation(audioFile.absolutePath)
                val mediaInformation = mediaInfoSession.mediaInformation
                if (mediaInformation == null) {
                    throw IllegalStateException("无法获取媒体信息：${audioFile.absolutePath}")
                }
                val streams = mediaInformation.streams
                val hasAudio = streams.any { it.type == "audio" }
                if (!hasAudio) {
                    throw IllegalStateException("视频文件不包含音频流，无法进行语音转写")
                }
            } catch (e: Exception) {
                 if (e is IllegalStateException) throw e
                 throw IllegalStateException("媒体文件检查失败: ${e.message}")
            }

            // 1. Convert to 16kHz wav
            wavFile = convertToWav(audioFile)
            
            // 2. Use recognizer safely with Mutex
            val resultText = mutex.withLock {
                val r = recognizer ?: getRecognizer().also { recognizer = it }
                
                // 3. Read wave data
                val waveData = WaveReader.readWaveFromFile(wavFile.absolutePath)
                    ?: throw IllegalStateException("Failed to read wave file: ${wavFile.absolutePath}")
                
                // 4. Create stream and decode
                try {
                    stream = r.createStream()
                    stream!!.acceptWaveform(waveData.samples, waveData.sampleRate)
                    r.decode(stream!!)
                    
                    // 5. Get result
                    val result = r.getResult(stream!!)
                    Log.d(TAG, "Transcription result length: ${result.text.length}")
                    result.text
                } catch (e: Throwable) {
                    Log.e(TAG, "Native recognition failed", e)
                    throw IllegalStateException("语音识别引擎出错: ${e.message}")
                }
            }
            
            return@withContext resultText
        } catch (e: Throwable) {
            Log.e(TAG, "Transcription process failed", e)
            throw if (e is Exception) e else RuntimeException("Transcription failed with fatal error: ${e.message}", e)
        } finally {
            // Clean up stream
            try {
                stream?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to release stream", e)
            }
            // Clean up temp wav file
            try {
                if (wavFile != null && wavFile.exists()) {
                    wavFile.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temp wav file", e)
            }
        }
    }

    private fun convertToWav(inputFile: File): File {
        // Use external cache dir if available, as it's often more reliable for native tools
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val outputFile = File(cacheDir, "temp_audio_16k_${System.currentTimeMillis()}.wav")
        if (outputFile.exists()) outputFile.delete()

        // -y: Overwrite output files without asking
        // -i: Input file
        // -ac 1: Mono
        // -ar 16000: 16kHz sample rate
        // -f wav: WAV format
        val args = arrayOf(
            "-y",
            "-i",
            inputFile.absolutePath,
            "-ac",
            "1",
            "-ar",
            "16000",
            "-f",
            "wav",
            outputFile.absolutePath
        )
        
        Log.d(TAG, "Executing FFmpeg command: ${args.joinToString(" ")}")
        val session = FFmpegKit.executeWithArguments(args)
        
        if (ReturnCode.isSuccess(session.returnCode)) {
            Log.d(TAG, "FFmpeg conversion successful: ${outputFile.absolutePath}")
            return outputFile
        } else {
            val log = session.allLogsAsString
            val returnCode = session.returnCode
            val errorLines = log.lines().takeLast(20).joinToString("\n")
            Log.e(TAG, "FFmpeg conversion failed (Code: $returnCode). Last lines:\n$errorLines")
            throw RuntimeException("FFmpeg 转换失败 (Code: $returnCode). 请检查日志。")
        }
    }
    
    suspend fun release() {
        mutex.withLock {
            try {
                recognizer?.release()
                recognizer = null
                Log.d(TAG, "Sherpa-onnx released")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release recognizer", e)
            }
        }
    }
}
