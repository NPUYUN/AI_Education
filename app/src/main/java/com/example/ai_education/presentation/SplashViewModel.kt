package com.example.ai_education.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.manager.VoskModelManager
import com.example.video_summarizer.data.downloader.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val voskModelManager: VoskModelManager,
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    private val _sherpaReady = MutableStateFlow(false)
    val sherpaReady: StateFlow<Boolean> = _sherpaReady.asStateFlow()

    private val _sherpaError = MutableStateFlow<String?>(null)
    val sherpaError: StateFlow<String?> = _sherpaError.asStateFlow()

    private val _sherpaProgress = MutableStateFlow<com.example.video_summarizer.data.downloader.DownloadProgress?>(null)
    val sherpaProgress: StateFlow<com.example.video_summarizer.data.downloader.DownloadProgress?> = _sherpaProgress.asStateFlow()

    init {
        voskModelManager.initModel()
        initSherpaModel()
    }

    fun retryVosk() {
        voskModelManager.initModel()
    }

    fun initSherpaModel() {
        _sherpaError.value = null
        viewModelScope.launch {
            if (!modelDownloader.isModelReady()) {
                modelDownloader.downloadAndExtractModel { progress ->
                    _sherpaProgress.value = progress
                }.fold(
                    onSuccess = {
                        _sherpaReady.value = true
                    },
                    onFailure = { error ->
                        _sherpaError.value = error.message
                    }
                )
            } else {
                _sherpaReady.value = true
            }
        }
    }
}