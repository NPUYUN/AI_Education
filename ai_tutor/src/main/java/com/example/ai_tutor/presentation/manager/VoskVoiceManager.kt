package com.example.ai_tutor.presentation.manager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

class VoskVoiceManager(private val context: Context) : RecognitionListener {

    private var speechService: SpeechService? = null
    private var model: Model? = null
    
    sealed class VoiceState {
        data class Loading(val message: String) : VoiceState()
        object Ready : VoiceState()
        object Listening : VoiceState()
        data class Result(val text: String) : VoiceState()
        data class Error(val error: String) : VoiceState()
    }

    private val _voiceState = Channel<VoiceState>(Channel.BUFFERED)
    val voiceState: Flow<VoiceState> = _voiceState.receiveAsFlow()

    fun init(scope: CoroutineScope) {
        scope.launch {
            try {
                _voiceState.send(VoiceState.Loading("正在检查语音模型..."))
                // Using 0.0f and "" as dummy values since we don't display progress in this init path anymore
                // Ideally init should be called after Splash has loaded the model.
                model = VoskModelManager.getModel(context) { _, msg ->
                    // scope.launch { _voiceState.send(VoiceState.Loading(msg)) }
                }
                
                if (model != null) {
                    _voiceState.send(VoiceState.Ready)
                } else {
                    _voiceState.send(VoiceState.Error("语音模型后台下载中"))
                }
            } catch (e: Exception) {
                _voiceState.send(VoiceState.Error("初始化失败: ${e.message}"))
            }
        }
    }

    fun startListening() {
        if (model == null) {
            model = VoskModelManager.getModel(context)
            if (model == null) {
                _voiceState.trySend(VoiceState.Error("模型仍在下载中，请稍后重试"))
                return
            }
        }
        
        try {
            if (speechService == null) {
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
            }
            speechService?.startListening(this)
            _voiceState.trySend(VoiceState.Listening)
        } catch (e: Exception) {
            _voiceState.trySend(VoiceState.Error("启动录音失败: ${e.message}"))
        }
    }

    fun stopListening() {
        speechService?.stop()
        // Do not nullify speechService here to reuse it? 
        // Vosk docs say: service.shutdown() when done completely.
        // stop() is enough to stop recording.
    }
    
    fun shutdown() {
        speechService?.shutdown()
        speechService = null
    }

    override fun onPartialResult(hypothesis: String?) {
        // Ignore partial results for now to avoid UI flicker
    }

    override fun onResult(hypothesis: String?) {
        processResult(hypothesis)
    }

    override fun onFinalResult(hypothesis: String?) {
        processResult(hypothesis)
        _voiceState.trySend(VoiceState.Ready)
    }

    private fun processResult(jsonString: String?) {
        jsonString?.let {
            try {
                val json = JSONObject(it)
                val text = json.optString("text", "").trim()
                if (text.isNotEmpty()) {
                    _voiceState.trySend(VoiceState.Result(text))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onError(exception: Exception?) {
        _voiceState.trySend(VoiceState.Error(exception?.message ?: "未知错误"))
    }

    override fun onTimeout() {
        _voiceState.trySend(VoiceState.Error("录音超时"))
    }
}
