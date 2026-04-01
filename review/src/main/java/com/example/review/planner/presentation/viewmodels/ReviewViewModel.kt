package com.example.review.planner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.PreferencesManager
import com.example.common.database.dao.ChatDao
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.models.ErrorBookEntity
import com.example.common.utils.NetworkMonitor
import com.example.common.utils.toUserFriendlyMessage
import com.example.review.planner.services.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val selectedTab: Int = 0,
    // Planner
    val reviewPlan: String = "",
    val isGeneratingPlan: Boolean = false,
    val subjectInput: String = "",
    val useRecentContextForPlan: Boolean = true,
    // Reinforcement
    val knowledgePointInput: String = "",
    val reinforcementQuiz: String = "",
    val isGeneratingQuiz: Boolean = false,
    val useRecentContextForQuiz: Boolean = true,
    // Error Book
    val errorRecords: List<ErrorBookEntity> = emptyList(),
    val selectedErrorIds: Set<Long> = emptySet(),
    
    // Practice Screen
    val isGeneratingPractice: Boolean = false,
    val practiceContent: String = "",
    val practiceAnswerInput: String = "",
    val isGradingPractice: Boolean = false,
    val practiceGradingResult: String = "",
    val showPracticeScreen: Boolean = false,
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val errorBookDao: ErrorBookDao,
        private val chatDao: ChatDao,
        private val globalConfigRepository: GlobalConfigRepository,
        private val preferencesManager: PreferencesManager,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewUiState())
        val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

        private val _errorEvents = kotlinx.coroutines.channels.Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

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
            _uiState.update { it.copy(selectedTab = index) }
        }

        fun updateSubjectInput(input: String) {
            _uiState.update { it.copy(subjectInput = input) }
            viewModelScope.launch {
                preferencesManager.saveString("review_subject_input", input)
            }
        }

        fun toggleUseRecentContextForPlan(use: Boolean) {
            _uiState.update { it.copy(useRecentContextForPlan = use) }
        }

        fun updateKnowledgePointInput(input: String) {
            _uiState.update { it.copy(knowledgePointInput = input) }
            viewModelScope.launch {
                preferencesManager.saveString("review_knowledge_point_input", input)
            }
        }

        fun toggleUseRecentContextForQuiz(use: Boolean) {
            _uiState.update { it.copy(useRecentContextForQuiz = use) }
        }

        private suspend fun getRecentContextString(): String {
            val sessions = chatDao.getSessions("current_user").firstOrNull() ?: emptyList()
            val recentSessions = sessions.take(3)
            val contextBuilder = StringBuilder()
            
            if (recentSessions.isNotEmpty()) {
                contextBuilder.append("【最近对话记录】\n")
                for (session in recentSessions) {
                    val msgs = chatDao.getMessages(session.id).firstOrNull() ?: emptyList()
                    val msgsText = msgs.takeLast(10).joinToString("\n") { "${if(it.role == "user") "学生" else "AI"}: ${it.content.take(50)}" }
                    if (msgsText.isNotBlank()) {
                        contextBuilder.append("对话：${session.title}\n$msgsText\n\n")
                    }
                }
            }

            val recentErrors = errorBookDao.getAllErrorRecords().firstOrNull()?.take(5) ?: emptyList()
            if (recentErrors.isNotEmpty()) {
                contextBuilder.append("【最近错题记录】\n")
                for (error in recentErrors) {
                    contextBuilder.append("科目：${error.subject}，题目：${error.questionContent}\n错误原因：${error.errorReason}\n\n")
                }
            }

            return contextBuilder.toString()
        }

        fun generateReviewPlan() {
            val subjectsInput = _uiState.value.subjectInput
            val useRecent = _uiState.value.useRecentContextForPlan
            
            if (subjectsInput.isBlank() && !useRecent) {
                viewModelScope.launch { _errorEvents.send("请输入复习科目，或选择基于最近记录生成") }
                return
            }
            if (!networkMonitor.isConnected.value) {
                viewModelScope.launch { _errorEvents.send("当前处于无网络环境，无法生成复习计划。") }
                return
            }

            _uiState.update { it.copy(isGeneratingPlan = true, reviewPlan = "") }
            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val recentContext = if (useRecent) getRecentContextString() else ""
                    val result = repository.generateReviewPlan(apiKey, baseUrl, modelName, subjectsInput, recentContext)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGeneratingPlan = false, reviewPlan = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg = exception?.toUserFriendlyMessage() ?: "未知错误"
                        _uiState.update { it.copy(isGeneratingPlan = false) }
                        _errorEvents.send(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.toUserFriendlyMessage()
                    _uiState.update { it.copy(isGeneratingPlan = false) }
                    _errorEvents.send(errorMsg)
                }
            }
        }

        fun generateReinforcementQuiz() {
            val kp = _uiState.value.knowledgePointInput
            val useRecent = _uiState.value.useRecentContextForQuiz

            if (kp.isBlank() && !useRecent) {
                viewModelScope.launch { _errorEvents.send("请输入要巩固的知识点，或选择基于最近记录生成") }
                return
            }
            if (!networkMonitor.isConnected.value) {
                viewModelScope.launch { _errorEvents.send("当前处于无网络环境，无法生成巩固练习。") }
                return
            }

            _uiState.update { it.copy(isGeneratingQuiz = true, reinforcementQuiz = "") }
            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val recentContext = if (useRecent) getRecentContextString() else ""
                    val result = repository.generateReinforcementQuiz(apiKey, baseUrl, modelName, kp, recentContext)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGeneratingQuiz = false, reinforcementQuiz = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg = exception?.toUserFriendlyMessage() ?: "未知错误"
                        _uiState.update { it.copy(isGeneratingQuiz = false) }
                        _errorEvents.send(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.toUserFriendlyMessage()
                    _uiState.update { it.copy(isGeneratingQuiz = false) }
                    _errorEvents.send(errorMsg)
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

        fun toggleErrorSelection(id: Long) {
            _uiState.update { state ->
                val newSelection = state.selectedErrorIds.toMutableSet()
                if (newSelection.contains(id)) {
                    newSelection.remove(id)
                } else {
                    newSelection.add(id)
                }
                state.copy(selectedErrorIds = newSelection)
            }
        }

        fun clearErrorSelection() {
            _uiState.update { it.copy(selectedErrorIds = emptySet()) }
        }

        fun selectAllErrors() {
            _uiState.update { state ->
                state.copy(selectedErrorIds = state.errorRecords.map { it.id }.toSet())
            }
        }

        fun startRedoPractice() {
            val selectedIds = _uiState.value.selectedErrorIds
            if (selectedIds.isEmpty()) return
            
            val selectedRecords = _uiState.value.errorRecords.filter { selectedIds.contains(it.id) }
            val practiceContent = selectedRecords.mapIndexed { index, record ->
                "**第 ${index + 1} 题（${record.subject}）**\n${record.questionContent}"
            }.joinToString("\n\n")

            _uiState.update { 
                it.copy(
                    practiceContent = practiceContent,
                    practiceAnswerInput = "",
                    practiceGradingResult = "",
                    isGeneratingPractice = false,
                    showPracticeScreen = true
                )
            }
        }

        fun generateSimilarPractice(count: Int) {
            val selectedIds = _uiState.value.selectedErrorIds
            if (selectedIds.isEmpty()) return

            val selectedRecords = _uiState.value.errorRecords.filter { selectedIds.contains(it.id) }
            val sourceProblems = selectedRecords.map { it.questionContent }

            _uiState.update { 
                it.copy(
                    isGeneratingPractice = true, 
                    practiceContent = "",
                    practiceAnswerInput = "",
                    practiceGradingResult = "",
                    showPracticeScreen = true
                ) 
            }

            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val result = repository.generateSimilarProblems(apiKey, baseUrl, modelName, sourceProblems, count)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGeneratingPractice = false, practiceContent = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg = exception?.toUserFriendlyMessage() ?: "未知错误"
                        _uiState.update { it.copy(isGeneratingPractice = false) }
                        _errorEvents.send(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.toUserFriendlyMessage()
                    _uiState.update { it.copy(isGeneratingPractice = false) }
                    _errorEvents.send(errorMsg)
                }
            }
        }

        fun closePracticeScreen() {
            _uiState.update { it.copy(showPracticeScreen = false) }
        }

        fun updatePracticeAnswer(answer: String) {
            _uiState.update { it.copy(practiceAnswerInput = answer) }
        }

        fun gradePractice() {
            val content = _uiState.value.practiceContent
            val answer = _uiState.value.practiceAnswerInput
            
            if (content.isBlank() || answer.isBlank()) {
                viewModelScope.launch { _errorEvents.send("题目或答案不能为空") }
                return
            }

            _uiState.update { it.copy(isGradingPractice = true, practiceGradingResult = "") }
            
            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val combinedInput = "【题目内容】\n$content\n\n【学生答案】\n$answer"
                    val result = repository.gradeTest(apiKey, baseUrl, modelName, combinedInput)

                    if (result.isSuccess) {
                        _uiState.update { it.copy(isGradingPractice = false, practiceGradingResult = result.getOrNull() ?: "") }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg = exception?.toUserFriendlyMessage() ?: "未知错误"
                        _uiState.update { it.copy(isGradingPractice = false) }
                        _errorEvents.send(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.toUserFriendlyMessage()
                    _uiState.update { it.copy(isGradingPractice = false) }
                    _errorEvents.send(errorMsg)
                }
            }
        }
    }
