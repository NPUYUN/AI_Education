package com.example.review.planner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.PreferencesManager
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.models.ErrorBookEntity
import com.example.common.utils.NetworkMonitor
import com.example.review.planner.services.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

data class ReviewUiState(
    val selectedTab: Int = 0,
    // Planner
    val reviewPlan: String = "",
    val isGeneratingPlan: Boolean = false,
    val subjectInput: String = "",
    // Reinforcement
    val knowledgePointInput: String = "",
    val reinforcementQuiz: String = "",
    val isGeneratingQuiz: Boolean = false,
    // Error Book
    val errorRecords: List<ErrorBookEntity> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val errorBookDao: ErrorBookDao,
        private val globalConfigRepository: GlobalConfigRepository,
        private val preferencesManager: PreferencesManager,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewUiState())
        val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                errorBookDao.getAllErrorRecords().collect { records ->
                    _uiState.update { it.copy(errorRecords = records) }
                }
            }
            viewModelScope.launch {
                preferencesManager.getString("review_subject_input", "数学,物理,英语").collect { savedInput ->
                    if (_uiState.value.subjectInput.isBlank()) {
                        _uiState.update { it.copy(subjectInput = savedInput) }
                    }
                }
            }
            viewModelScope.launch {
                preferencesManager.getString("review_knowledge_point_input", "").collect { savedInput ->
                    if (_uiState.value.knowledgePointInput.isBlank()) {
                        _uiState.update { it.copy(knowledgePointInput = savedInput) }
                    }
                }
            }
        }

        fun setTab(index: Int) {
            _uiState.update { it.copy(selectedTab = index, error = null) }
        }

        fun updateSubjectInput(input: String) {
            _uiState.update { it.copy(subjectInput = input) }
            viewModelScope.launch {
                preferencesManager.saveString("review_subject_input", input)
            }
        }

        fun updateKnowledgePointInput(input: String) {
            _uiState.update { it.copy(knowledgePointInput = input) }
            viewModelScope.launch {
                preferencesManager.saveString("review_knowledge_point_input", input)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun generateReviewPlan() {
            val subjectsInput = _uiState.value.subjectInput
            if (subjectsInput.isBlank()) {
                _uiState.update { it.copy(error = "请输入复习科目") }
                return
            }
            if (!networkMonitor.isConnected.value) {
                _uiState.update { it.copy(error = "当前处于无网络环境，无法生成复习计划。") }
                return
            }
            val subjects = subjectsInput.split("[,，、]".toRegex()).map { it.trim() }.filter { it.isNotBlank() }

            _uiState.update { it.copy(isGeneratingPlan = true, error = null, reviewPlan = "") }
            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val result = repository.generateReviewPlan(apiKey, baseUrl, modelName, subjects)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGeneratingPlan = false, reviewPlan = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg =
                            when (exception) {
                                is SocketTimeoutException -> "网络请求超时，请稍后重试"
                                is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                                else -> exception?.message ?: "未知错误"
                            }
                        _uiState.update { it.copy(isGeneratingPlan = false, error = errorMsg) }
                    }
                } catch (e: Exception) {
                    val errorMsg =
                        when (e) {
                            is SocketTimeoutException -> "网络请求超时，请稍后重试"
                            is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                            else -> e.message ?: "发生异常"
                        }
                    _uiState.update { it.copy(isGeneratingPlan = false, error = errorMsg) }
                }
            }
        }

        fun generateReinforcementQuiz() {
            val kp = _uiState.value.knowledgePointInput
            if (kp.isBlank()) {
                _uiState.update { it.copy(error = "请输入要巩固的知识点") }
                return
            }
            if (!networkMonitor.isConnected.value) {
                _uiState.update { it.copy(error = "当前处于无网络环境，无法生成巩固练习。") }
                return
            }

            _uiState.update { it.copy(isGeneratingQuiz = true, error = null, reinforcementQuiz = "") }
            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val result = repository.generateReinforcementQuiz(apiKey, baseUrl, modelName, kp)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGeneratingQuiz = false, reinforcementQuiz = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg =
                            when (exception) {
                                is SocketTimeoutException -> "网络请求超时，请稍后重试"
                                is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                                else -> exception?.message ?: "未知错误"
                            }
                        _uiState.update { it.copy(isGeneratingQuiz = false, error = errorMsg) }
                    }
                } catch (e: Exception) {
                    val errorMsg =
                        when (e) {
                            is SocketTimeoutException -> "网络请求超时，请稍后重试"
                            is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                            else -> e.message ?: "发生异常"
                        }
                    _uiState.update { it.copy(isGeneratingQuiz = false, error = errorMsg) }
                }
            }
        }

        // For demonstration, adding a mock error record
        fun addMockErrorRecord() {
            viewModelScope.launch {
                val record =
                    ErrorBookEntity(
                        subject = "数学",
                        questionContent = "求函数 f(x) = x^2 - 4x + 3 的最小值。",
                        errorReason = "计算顶点坐标时符号错误",
                        correctSolution = "f(x) = (x-2)^2 - 1，所以最小值为 -1。",
                    )
                errorBookDao.insertErrorRecord(record)
            }
        }

        fun deleteErrorRecord(record: ErrorBookEntity) {
            viewModelScope.launch {
                errorBookDao.deleteErrorRecord(record)
            }
        }
    }
