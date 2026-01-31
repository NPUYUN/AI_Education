package com.example.ai_tutor.presentation.manager

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(onError: (String) -> Unit) {
        try {
            outputFile = File(context.cacheDir, "voice_input.m4a")
            
            mediaRecorder = createMediaRecorder()

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000) // 16kHz recommended for ASR
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e("AudioRecorder", "prepare() failed", e)
            onError("录音启动失败: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "start() failed", e)
            onError("录音失败: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed", e)
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            outputFile?.delete()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
