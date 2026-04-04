package com.example.review.planner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.PreferencesManager
import com.example.common.database.dao.ChatDao
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.dao.ReviewHistoryDao
import com.example.common.database.models.ErrorBookEntity
import com.example.common.database.models.ReviewHistoryEntity
import com.example.common.utils.NetworkMonitor
import com.example.common.utils.toUserFriendlyMessage
import com.example.review.planner.services.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.review.planner.models.GeneratedProblem

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
    val practiceProblems: List<GeneratedProblem> = emptyList(),
    val currentPracticeIndex: Int = 0,
    val practiceAnswers: Map<Int, String> = emptyMap(), // map of index to answer
    val isGradingPractice: Boolean = false,
    val practiceGradingResults: List<PracticeGradingResult> = emptyList(), // grading result per question
    val showPracticeScreen: Boolean = false,
    val showPracticeResultScreen: Boolean = false,
    val showPracticeHistoryForRecordId: Long? = null,
    // History
    val plannerHistory: List<ReviewHistoryEntity> = emptyList(),
    val reinforcementHistory: List<ReviewHistoryEntity> = emptyList(),
    val practiceHistory: List<ReviewHistoryEntity> = emptyList(),
)

data class PracticeGradingResult(
    val isCorrect: Boolean,
    val score: Int,
    val explanation: String
)

@HiltViewModel
class ReviewViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val errorBookDao: ErrorBookDao,
        private val chatDao: ChatDao,
        private val reviewHistoryDao: ReviewHistoryDao,
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
                reviewHistoryDao.getAllHistory().collect { allHistory ->
                    val plannerHistory = allHistory.filter { it.type == "planner" }
                    val reinforcementHistory = allHistory.filter { it.type == "reinforcement" }
                    val practiceHistory = allHistory.filter { it.type.startsWith("practice_") }
                    _uiState.update { 
                        it.copy(
                            plannerHistory = plannerHistory,
                            reinforcementHistory = reinforcementHistory,
                            practiceHistory = practiceHistory
                        ) 
                    }
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
                    val msgsText =
                        msgs.takeLast(
                            10,
                        ).joinToString("\n") { "${if (it.role == "user") "学生" else "AI"}: ${it.content.take(50)}" }
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
                        val planContent = result.getOrNull() ?: ""
                        _uiState.update { it.copy(isGeneratingPlan = false, reviewPlan = planContent) }

                        // Save history
                        val inputParams = if (useRecent) "科目需求: $subjectsInput\n[包含最近学习记录]" else "科目需求: $subjectsInput"
                        reviewHistoryDao.insertHistory(
                            ReviewHistoryEntity(
                                type = "planner",
                                inputParameters = inputParams,
                                resultContent = planContent,
                            ),
                        )
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
                        val quizContent = result.getOrNull() ?: ""
                        _uiState.update { it.copy(isGeneratingQuiz = false, reinforcementQuiz = quizContent) }

                        // Save history
                        val inputParams = if (useRecent) "知识点/需求: $kp\n[包含最近学习记录]" else "知识点/需求: $kp"
                        reviewHistoryDao.insertHistory(
                            ReviewHistoryEntity(
                                type = "reinforcement",
                                inputParameters = inputParams,
                                resultContent = quizContent,
                            ),
                        )
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

        fun loadPlannerHistory(historyEntity: ReviewHistoryEntity) {
            _uiState.update { it.copy(reviewPlan = historyEntity.resultContent) }
        }

        fun loadReinforcementHistory(historyEntity: ReviewHistoryEntity) {
            _uiState.update { it.copy(reinforcementQuiz = historyEntity.resultContent) }
        }

        fun deleteHistory(historyEntity: ReviewHistoryEntity) {
            viewModelScope.launch {
                reviewHistoryDao.deleteHistory(historyEntity)
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

        fun generatePracticeFromErrors(count: Int) {
            val selectedIds = _uiState.value.selectedErrorIds
            if (selectedIds.isEmpty()) return

            val selectedRecords = _uiState.value.errorRecords.filter { selectedIds.contains(it.id) }

            _uiState.update {
                it.copy(
                    isGeneratingPractice = true,
                    practiceProblems = emptyList(),
                    currentPracticeIndex = 0,
                    practiceAnswers = emptyMap(),
                    isGradingPractice = false,
                    practiceGradingResults = emptyList(),
                    showPracticeResultScreen = false,
                )
            }

            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val result = repository.generateSimilarProblems(apiKey, baseUrl, modelName, selectedRecords, count)

                    if (result.isSuccess) {
                        _uiState.update { 
                            it.copy(
                                isGeneratingPractice = false, 
                                practiceProblems = result.getOrNull() ?: emptyList(),
                                showPracticeScreen = true
                            ) 
                        }
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
            _uiState.update { it.copy(showPracticeScreen = false, showPracticeResultScreen = false) }
        }
        
        fun closePracticeResultScreen() {
            _uiState.update { it.copy(showPracticeResultScreen = false, showPracticeScreen = false) }
        }

        fun openPracticeHistory(recordId: Long) {
            _uiState.update { it.copy(showPracticeHistoryForRecordId = recordId) }
        }

        fun closePracticeHistory() {
            _uiState.update { it.copy(showPracticeHistoryForRecordId = null) }
        }

        fun loadPracticeHistoryDetail(history: ReviewHistoryEntity) {
            try {
                val typeToken = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = com.google.gson.Gson().fromJson(history.resultContent, typeToken)
                
                val problemsStr = com.google.gson.Gson().toJson(data["problems"])
                val problems = com.google.gson.Gson().fromJson(problemsStr, Array<GeneratedProblem>::class.java).toList()
                
                val resultsStr = com.google.gson.Gson().toJson(data["results"])
                val results = com.google.gson.Gson().fromJson(resultsStr, Array<PracticeGradingResult>::class.java).toList()
                
                val answersStr = com.google.gson.Gson().toJson(data["answers"])
                val answersTypeToken = object : com.google.gson.reflect.TypeToken<Map<Int, String>>() {}.type
                val answers: Map<Int, String> = com.google.gson.Gson().fromJson(answersStr, answersTypeToken)
                
                _uiState.update { 
                    it.copy(
                        practiceProblems = problems,
                        practiceGradingResults = results,
                        practiceAnswers = answers,
                        showPracticeResultScreen = true,
                        showPracticeHistoryForRecordId = null // close the list overlay
                    ) 
                }
            } catch (e: Exception) {
                viewModelScope.launch { _errorEvents.send("加载历史记录失败") }
            }
        }

        fun updatePracticeAnswer(answer: String) {
            _uiState.update { state ->
                val newAnswers = state.practiceAnswers.toMutableMap()
                newAnswers[state.currentPracticeIndex] = answer
                state.copy(practiceAnswers = newAnswers)
            }
        }

        fun setCurrentPracticeIndex(index: Int) {
            _uiState.update { it.copy(currentPracticeIndex = index) }
        }

        fun submitPractice() {
            val state = _uiState.value
            val problems = state.practiceProblems
            val answers = state.practiceAnswers
            
            val problemsAndAnswers = problems.mapIndexed { index, problem ->
                val ans = answers[index] ?: "未作答"
                Pair(problem, ans)
            }

            _uiState.update { it.copy(isGradingPractice = true, practiceGradingResults = emptyList()) }

            viewModelScope.launch {
                try {
                    val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull() ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull() ?: AppConstants.BASE_URL

                    val result = repository.gradeTest(apiKey, baseUrl, modelName, problemsAndAnswers)

                    if (result.isSuccess) {
                        val results = result.getOrNull() ?: emptyList()
                        _uiState.update { 
                            it.copy(
                                isGradingPractice = false, 
                                practiceGradingResults = results,
                                showPracticeResultScreen = true,
                                showPracticeScreen = false
                            ) 
                        }
                        
                        // Save history
                        val correctCount = results.count { it.isCorrect }
                        val accuracy = if (problems.isNotEmpty()) (correctCount * 100 / problems.size) else 0
                        
                        val historyData = mapOf(
                            "problems" to problems,
                            "results" to results,
                            "answers" to answers,
                            "accuracy" to accuracy,
                            "totalScore" to results.sumOf { it.score }
                        )
                        val historyContent = com.google.gson.Gson().toJson(historyData)
                        
                        val selectedIdsStr = _uiState.value.selectedErrorIds.joinToString(",")
                        reviewHistoryDao.insertHistory(
                            ReviewHistoryEntity(
                                type = "practice_$selectedIdsStr",
                                inputParameters = "错题变式测试",
                                resultContent = historyContent,
                            ),
                        )
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
        
        fun addGeneratedProblemToErrorBook(problem: GeneratedProblem) {
            viewModelScope.launch {
                try {
                    val entity = ErrorBookEntity(
                        subject = problem.knowledgePointId ?: "变式训练",
                        questionContent = problem.questionText + if (!problem.options.isNullOrEmpty()) "\n\n" + problem.options.joinToString("\n") else "",
                        errorReason = "变式训练生成题目",
                        correctSolution = problem.answer + "\n\n" + problem.explanation,
                        timestamp = System.currentTimeMillis()
                    )
                    errorBookDao.insertErrorRecord(entity)
                    _errorEvents.send("已添加到错题本")
                } catch (e: Exception) {
                    _errorEvents.send("添加失败: ${e.message}")
                }
            }
        }
    }

