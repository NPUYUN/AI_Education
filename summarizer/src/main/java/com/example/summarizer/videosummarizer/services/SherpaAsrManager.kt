package com.example.summarizer.videosummarizer.services

import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.common.dispatchers.DispatcherProvider
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class SherpaAsrManager(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {
    private var recognizer: OfflineRecognizer? = null
    private val tag = "SherpaAsrManager"
    private val mutex = Mutex()

    // Initialize the recognizer lazily (Must be called within mutex lock)
    private fun getRecognizer(): OfflineRecognizer {
        if (recognizer == null) {
            try {
                // Check if context/resources are still valid before initializing Native memory
                val baseDir = context.externalCacheDir ?: context.cacheDir
                val modelDir = File(baseDir, "sherpa-onnx-paraformer-zh-2023-09-14").absolutePath

                if (!File(modelDir).exists() || !File(modelDir, "model.int8.onnx").exists() || !File(modelDir, "tokens.txt").exists()) {
                    throw IllegalStateException("语音识别模型未下载或不完整，请先下载模型")
                }

                // Make sure to load the correct paths
                val modelPath = File(modelDir, "model.int8.onnx").absolutePath
                val tokensPath = File(modelDir, "tokens.txt").absolutePath

                val config =
                    OfflineRecognizerConfig(
                        modelConfig =
                            OfflineModelConfig(
                                paraformer =
                                    OfflineParaformerModelConfig(
                                        model = modelPath,
                                    ),
                                tokens = tokensPath,
                                modelType = "paraformer",
                                debug = false,
                                numThreads = 1, // Limit to 1 thread to completely avoid thread-related crashes in underlying ONNX
                            ),
                    )

                recognizer =
                    OfflineRecognizer(
                        assetManager = null, // Set to null to load from file paths instead of assets
                        config = config,
                    )
                Log.d(tag, "Sherpa-onnx initialized successfully from file path: $modelDir")
            } catch (e: Throwable) {
                Log.e(tag, "Failed to initialize Sherpa-onnx", e)
                val detail =
                    buildString {
                        append(e.message ?: e.javaClass.simpleName)
                        e.cause?.let { c ->
                            append(" | ")
                            append(c.message ?: c.javaClass.simpleName)
                        }
                    }
                throw IllegalStateException("语音识别初始化失败: $detail", e)
            }
        }
        return recognizer!!
    }

    suspend fun transcribe(audioFile: File): String =
        withContext(dispatcherProvider.io) {
            // FFmpeg 与 Sherpa 原生调用必须串行：此前 convertToWav 在锁外执行，并发转写时易触发 native 崩溃
            mutex.withLock {
                var wavFile: File? = null
                var stream: OfflineStream? = null
                try {
                    Log.d(tag, "Starting transcription for: ${audioFile.name}")

                    wavFile = convertToWav(audioFile)

                    // Ensure recognizer is initialized properly before proceeding
                    val r =
                        recognizer ?: try {
                            getRecognizer().also { recognizer = it }
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to initialize recognizer", e)
                            throw IllegalStateException("语音识别引擎初始化失败", e)
                        }

                    // Protect against NPE if getRecognizer() still returned null or recognizer field became null
                    if (r == null) {
                        throw IllegalStateException("语音识别引擎未正确初始化")
                    }

                    // 彻底避开 JNI WaveReader 的内存陷阱。
                    // 原先底层一次性读取整个音频导致 C++ 和 ART 虚拟机内存爆炸 (Fatal signal 6, Channel unrecoverably broken)。
                    // 现在改为在 Kotlin 层直接读取 WAV 并分块转 FloatArray，每次仅加载约 30 秒 (3MB) 到内存，真正实现流式处理。
                    val maxSegmentDurationSeconds = 30
                    val sampleRate = 16000
                    val segmentSize = sampleRate * maxSegmentDurationSeconds // 480,000 samples per chunk
                    val bytesPerSegment = segmentSize * 2 // 16-bit PCM = 2 bytes per sample

                    val fullText = StringBuilder()

                    java.io.RandomAccessFile(wavFile, "r").use { raf ->
                        // 解析 WAV 头寻找 data chunk 起始位置，避免硬编码 44 字节带来的兼容性问题
                        var dataOffset = 44L
                        try {
                            raf.seek(12) // Skip "RIFF" + size + "WAVE"
                            while (raf.filePointer < raf.length()) {
                                val chunkId = ByteArray(4)
                                raf.readFully(chunkId)
                                val chunkSize = Integer.reverseBytes(raf.readInt()) // little-endian
                                if (String(chunkId) == "data") {
                                    dataOffset = raf.filePointer
                                    break
                                }
                                raf.seek(raf.filePointer + chunkSize + (chunkSize % 2))
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to parse WAV header precisely, fallback to offset 44", e)
                        }

                        raf.seek(dataOffset)
                        val byteBuffer = ByteArray(bytesPerSegment)

                        while (true) {
                            kotlin.coroutines.coroutineContext.ensureActive()

                            val bytesRead = raf.read(byteBuffer)
                            if (bytesRead <= 0) break

                            val samplesCount = bytesRead / 2
                            val chunkFloatArray = FloatArray(samplesCount)

                            // 16-bit PCM (Little Endian) to Float [-1.0, 1.0]
                            for (i in 0 until samplesCount) {
                                val low = byteBuffer[i * 2].toInt() and 0xFF
                                val high = byteBuffer[i * 2 + 1].toInt()
                                val sample = (high shl 8) or low
                                // Handle sign extension for 16-bit integer
                                val signedSample = sample.toShort().toInt()
                                chunkFloatArray[i] = signedSample / 32768.0f
                            }

                            var segmentStream: OfflineStream? = null
                            try {
                                segmentStream = r.createStream()
                                if (segmentStream == null) {
                                    throw IllegalStateException("Failed to create OfflineStream from Native engine.")
                                }

                                segmentStream.acceptWaveform(chunkFloatArray, sampleRate)

                                kotlin.coroutines.coroutineContext.ensureActive()
                                r.decode(segmentStream)
                                kotlin.coroutines.coroutineContext.ensureActive()

                                val result = r.getResult(segmentStream)
                                if (result != null && result.text.isNotBlank() && !result.text.contains("【语音识别底层返回异常】")) {
                                    fullText.append(result.text)
                                }
                            } finally {
                                try {
                                    segmentStream?.release()
                                } catch (e: Exception) {
                                    Log.w(tag, "Failed to release segment stream", e)
                                }
                            }
                        }
                    }

                    val finalText = fullText.toString()
                    Log.d(tag, "Transcription completed. Total length: ${finalText.length}")
                    return@withLock finalText
                } catch (e: Throwable) {
                    try {
                        wavFile?.takeIf { it.exists() }?.delete()
                    } catch (_: Exception) {
                    }
                    throw e
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
        val args =
            arrayOf(
                "-y",
                "-i",
                inputFile.absolutePath,
                "-ac",
                "1",
                "-ar",
                "16000",
                "-f",
                "wav",
                outputFile.absolutePath,
            )

        Log.d(tag, "Executing FFmpeg command: ${args.joinToString(" ")}")
        val session =
            try {
                FFmpegKit.executeWithArguments(args)
            } catch (t: Throwable) {
                Log.e(tag, "FFmpegKit.executeWithArguments threw", t)
                throw IllegalStateException("音频提取失败: ${t.message ?: t.javaClass.simpleName}", t)
            }

        if (ReturnCode.isSuccess(session.returnCode)) {
            Log.d(tag, "FFmpeg conversion successful: ${outputFile.absolutePath}")
            return outputFile
        } else {
            val log = session.allLogsAsString
            val returnCode = session.returnCode
            val errorLines = log.lines().takeLast(20).joinToString("\n")
            Log.e(tag, "FFmpeg conversion failed (Code: $returnCode). Last lines:\n$errorLines")
            throw RuntimeException("FFmpeg 转换失败 (Code: $returnCode). 请检查日志。")
        }
    }

    suspend fun release() {
        mutex.withLock {
            try {
                recognizer?.release()
                recognizer = null
                Log.d(tag, "Sherpa-onnx released")
            } catch (e: Exception) {
                Log.e(tag, "Failed to release recognizer", e)
            }
        }
    }
}
