package com.example.ai_tutor.presentation.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.os.Handler
import android.os.Looper

class VoiceInputManager(private val context: Context) {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // Remove hardcoded language to allow system default, prevents errors if language pack missing
        // putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN") 
    }

    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() { _isListening.value = true }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { _isListening.value = false }
                override fun onError(error: Int) {
                    _isListening.value = false
                    val message = when(error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "未检测到语音"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误 (请重试)"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "服务繁忙"
                        else -> "语音识别错误: $error"
                    }
                    // Ignore "No Match" as it's common
                    if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                        onError?.invoke(message)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult?.invoke(matches[0])
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                     val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                     if (!matches.isNullOrEmpty()) {
                         // Optional: could expose partial results flow
                     }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (speechRecognizer == null) {
            onError("当前设备不支持语音识别")
            return
        }
        
        this.onResult = onResult
        this.onError = onError
        
        mainHandler.post {
            try {
                // Cancel any previous session to avoid ERROR_CLIENT
                speechRecognizer?.cancel() 
                speechRecognizer?.startListening(speechIntent)
            } catch (e: Exception) {
                onError(e.message ?: "启动失败")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            speechRecognizer?.stopListening()
        }
    }

    fun destroy() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
