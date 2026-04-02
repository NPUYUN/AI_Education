package com.example.solver.comprehensive.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ErrorBookDao
import com.example.common.database.dao.SolveHistoryDao
import com.example.common.database.models.ErrorBookEntity
import com.example.common.database.models.SolveHistoryEntity
import com.example.common.utils.NetworkMonitor
import com.example.common.utils.toUserFriendlyMessage
import com.example.solver.comprehensive.services.SolverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class SolverUiState(
    val selectedTab: Int = 0,
    val questionText: String = "",
    val imageUri: Uri? = null,
    val isSolving: Boolean = false,
    val solutionResult: String = "",
    val isAddedToErrorBook: Boolean = false,
    val drawingSteps: List<com.example.solver.geometry_solver.presentation.components.GeometryDrawingStep> = emptyList(),
    val isFunction: Boolean = false,
    val comprehensiveType: String = "",
)

@HiltViewModel
class SolverViewModel
    @Inject
    constructor(
        private val repository: SolverRepository,
        private val globalConfigRepository: GlobalConfigRepository,
        private val errorBookDao: ErrorBookDao,
        private val solveHistoryDao: SolveHistoryDao,
        private val networkMonitor: NetworkMonitor,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SolverUiState())
        val uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()

        private val _errorEvents = kotlinx.coroutines.channels.Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

        private var lastHistoryId: Long? = null

        val recentHistory = solveHistoryDao.getRecent(8)
        val allHistory = solveHistoryDao.getAll()

        fun setTab(index: Int) {
            _uiState.value =
                _uiState.value.copy(
                    selectedTab = index,
                    questionText = "",
                    imageUri = null,
                    solutionResult = "",
                    isAddedToErrorBook = false,
                )
        }

        fun updateQuestionText(text: String) {
            val newTab = classify(text)
            val compType = if (newTab == 2) getComprehensiveType(text) else ""
            _uiState.value =
                _uiState.value.copy(
                    questionText = text,
                    selectedTab = newTab,
                    isAddedToErrorBook = false,
                    isFunction = isFunctionProblem(text),
                    comprehensiveType = compType,
                )
        }

        fun setImageUri(uri: Uri?) {
            val newTab = classify(_uiState.value.questionText)
            val compType = if (newTab == 2) getComprehensiveType(_uiState.value.questionText) else ""
            _uiState.value =
                _uiState.value.copy(
                    imageUri = uri,
                    selectedTab = newTab,
                    isAddedToErrorBook = false,
                    comprehensiveType = compType,
                )
        }

        fun clearError() {
            // Deprecated: error events are now handled via Channel
        }

        fun addToErrorBook(
            subject: String,
            errorReason: String,
        ) {
            val state = _uiState.value
            if (state.solutionResult.isBlank()) return

            viewModelScope.launch {
                try {
                    val questionContent = if (state.questionText.isNotBlank()) state.questionText else "图片题目（暂无文字描述）"
                    val entity =
                        ErrorBookEntity(
                            subject = subject,
                            questionContent = questionContent,
                            errorReason = errorReason,
                            correctSolution = state.solutionResult,
                        )
                    errorBookDao.insertErrorRecord(entity)
                    lastHistoryId?.let { id ->
                        solveHistoryDao.markInErrorBook(id)
                    }
                    _uiState.value = _uiState.value.copy(isAddedToErrorBook = true)
                } catch (e: Exception) {
                    _errorEvents.send("保存到错题本失败: ${e.message}")
                }
            }
        }

        fun solveProblem() {
            val state = _uiState.value
            if (state.questionText.isBlank() && state.imageUri == null) {
                viewModelScope.launch {
                    _errorEvents.send("请输入题目或上传题目图片")
                }
                return
            }

            _uiState.value = state.copy(isSolving = true, solutionResult = "")

            viewModelScope.launch {
                if (!networkMonitor.isConnected.value) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSolving = false,
                        )
                    _errorEvents.send("当前处于无网络环境，大模型解题服务暂不可用。\n您可以查看历史解题记录和错题本，或连接网络后重试。")
                    return@launch
                }

                try {
                    val apiKey =
                        globalConfigRepository.getAiTutorApiKey().firstOrNull()
                            ?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                    val modelName =
                        globalConfigRepository.getAiTutorModelName().firstOrNull()
                            ?: AppConstants.DEFAULT_MODEL_NAME
                    val baseUrl =
                        globalConfigRepository.getAiTutorBaseUrl().firstOrNull()
                            ?: AppConstants.BASE_URL

                    val autoTab = classify(state.questionText)
                    val compType = if (autoTab == 2) getComprehensiveType(state.questionText) else ""
                    _uiState.value = _uiState.value.copy(selectedTab = autoTab, comprehensiveType = compType)
                    val systemPrompt =
                        when (autoTab) {
                            0 -> AppConstants.SOLVER_GEOMETRY_SYSTEM_PROMPT
                            1 -> AppConstants.SOLVER_ALGEBRA_SYSTEM_PROMPT
                            else ->
                                when (compType) {
                                    "物理" -> AppConstants.SOLVER_PHYSICS_SYSTEM_PROMPT
                                    "化学" -> AppConstants.SOLVER_CHEMISTRY_SYSTEM_PROMPT
                                    "生物" -> AppConstants.SOLVER_BIOLOGY_SYSTEM_PROMPT
                                    else -> AppConstants.SOLVER_COMPREHENSIVE_SYSTEM_PROMPT
                                }
                        }

                    var effectiveModelName = modelName
                    var base64Image: String? = null
                    if (state.imageUri != null) {
                        base64Image = encodeImage(state.imageUri)
                        if (base64Image == null) {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSolving = false,
                                )
                            _errorEvents.send("图片处理失败，请尝试重新选择或拍摄")
                            return@launch
                        }
                        // Force using vision model if image is provided
                        if (effectiveModelName.contains("qwen") && !effectiveModelName.contains("vl")) {
                            effectiveModelName = "qwen-vl-plus"
                        }
                    }

                    val result =
                        repository.solveProblem(
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            modelName = effectiveModelName,
                            systemPrompt = systemPrompt,
                            questionText = state.questionText,
                            base64Image = base64Image,
                        )

                    if (result.isSuccess) {
                        val rawSolution = result.getOrNull() ?: ""
                        val drawings = parseDrawingSteps(rawSolution)
                        val solution = cleanSolutionText(rawSolution)
                        _uiState.value =
                            _uiState.value.copy(
                                isSolving = false,
                                solutionResult = solution,
                                drawingSteps = drawings,
                                isFunction =
                                    if (_uiState.value.selectedTab == 1) {
                                        isFunctionProblem(
                                            _uiState.value.questionText,
                                        ) || solution.contains("函数") || solution.contains("y=") || solution.contains("f(")
                                    } else {
                                        _uiState.value.isFunction
                                    },
                            )
                        val subject =
                            when (_uiState.value.selectedTab) {
                                0 -> "几何"
                                1 -> "代数"
                                else -> "综合"
                            }
                        val history =
                            SolveHistoryEntity(
                                subject = subject,
                                questionContent = if (_uiState.value.questionText.isNotBlank()) _uiState.value.questionText else "图片题目（暂无文字描述）",
                                imageUri = _uiState.value.imageUri?.toString(),
                                solution = solution,
                            )
                        try {
                            lastHistoryId = solveHistoryDao.insert(history)
                        } catch (_: Exception) {
                            // ignore history save errors
                        }
                    } else {
                        val exception = result.exceptionOrNull()
                        val errorMsg = exception?.toUserFriendlyMessage() ?: "未知错误"
                        _uiState.value =
                            _uiState.value.copy(
                                isSolving = false,
                            )
                        _errorEvents.send(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.toUserFriendlyMessage()
                    _uiState.value =
                        _uiState.value.copy(
                            isSolving = false,
                        )
                    _errorEvents.send(errorMsg)
                }
            }
        }

        private fun getComprehensiveType(text: String): String {
            val t = text.replace("\\s+".toRegex(), "").lowercase()
            val physicsKeywords =
                listOf(
                    "物理",
                    "力",
                    "速度",
                    "加速度",
                    "质量",
                    "动能",
                    "势能",
                    "电场",
                    "磁场",
                    "电路",
                    "电阻",
                    "电压",
                    "电流",
                    "做功",
                    "功率",
                    "折射",
                    "反射",
                    "透镜",
                    "滑块",
                    "斜面",
                    "匀速",
                    "牛顿",
                    "摩擦",
                    "碰撞",
                    "波",
                    "动量",
                )
            val chemistryKeywords =
                listOf(
                    "化学",
                    "反应",
                    "溶液",
                    "沉淀",
                    "摩尔",
                    "氧化",
                    "还原",
                    "酸",
                    "碱",
                    "ph",
                    "离子",
                    "原子",
                    "分子",
                    "气体",
                    "催化剂",
                    "实验",
                    "试管",
                    "烧杯",
                    "方程式",
                    "浓度",
                    "有机",
                    "无机",
                    "结晶",
                    "滴定",
                    "溶解度",
                )
            val biologyKeywords =
                listOf(
                    "生物",
                    "细胞",
                    "基因",
                    "遗传",
                    "光合作用",
                    "呼吸作用",
                    "蛋白质",
                    "氨基酸",
                    "染色体",
                    "进化",
                    "生态",
                    "植物",
                    "动物",
                    "孟德尔",
                    "DNA",
                    "RNA",
                    "酶",
                    "激素",
                    "免疫",
                    "神经",
                    "代谢",
                )

            return when {
                physicsKeywords.any { t.contains(it) } -> "物理"
                chemistryKeywords.any { t.contains(it) } -> "化学"
                biologyKeywords.any { t.contains(it) } -> "生物"
                else -> "其他"
            }
        }

        private fun classify(text: String): Int {
            val t = text.lowercase()
            val geometryKeywords =
                listOf(
                    "角",
                    "三角",
                    "圆",
                    "半径",
                    "周长",
                    "面积",
                    "几何",
                    "垂线",
                    "相似",
                    "勾股",
                    "坐标",
                    "图形",
                    "直线",
                    "曲线",
                    "相切",
                    "相交",
                    "内切",
                    "外接",
                    "体积",
                    "表面积",
                    "平行",
                    "垂直",
                )
            val algebraKeywords =
                listOf(
                    "方程",
                    "一次",
                    "二次",
                    "函数",
                    "求值",
                    "解",
                    "x",
                    "y",
                    "代数",
                    "不等式",
                    "因式分解",
                    "化简",
                    "数列",
                    "等差",
                    "等比",
                    "多项式",
                    "根",
                    "极值",
                    "单调",
                    "导数",
                    "对数",
                    "指数",
                )
            return when {
                geometryKeywords.any { t.contains(it) } -> 0
                algebraKeywords.any { t.contains(it) } -> 1
                else -> 2
            }
        }

        private fun isFunctionProblem(text: String): Boolean {
            val t = text.replace("\\s+".toRegex(), "").lowercase()
            return t.contains(
                "函数",
            ) ||
                t.contains(
                    "y=",
                ) ||
                t.contains(
                    "f(",
                ) ||
                t.contains(
                    "曲线",
                ) ||
                t.contains(
                    "图像",
                ) ||
                t.contains(
                    "单调",
                ) ||
                t.contains(
                    "极值",
                ) ||
                t.contains(
                    "导数",
                ) || t.contains("抛物线") || t.contains("直线") || t.contains("指数函数") || t.contains("对数函数") || t.contains("三角函数")
        }

        private fun cleanSolutionText(text: String): String {
            val startToken = "BEGIN_DRAWING_JSON"
            val endToken = "END_DRAWING_JSON"
            val start = text.indexOf(startToken)
            val end = text.indexOf(endToken)
            if (start >= 0 && end > start) {
                // Remove the JSON block and the tags
                val cleaned = text.substring(0, start) + text.substring(end + endToken.length)
                return cleaned.trim()
            }
            return text
        }

        private fun parseDrawingSteps(text: String): List<com.example.solver.geometry_solver.presentation.components.GeometryDrawingStep> {
            try {
                val startToken = "BEGIN_DRAWING_JSON"
                val endToken = "END_DRAWING_JSON"
                val start = text.indexOf(startToken)
                val end = text.indexOf(endToken)
                if (start >= 0 && end > start) {
                    val json = text.substring(start + startToken.length, end).trim()
                    val gson = com.google.gson.Gson()
                    val type =
                        com.google.gson.reflect.TypeToken.getParameterized(
                            List::class.java,
                            com.example.solver.geometry_solver.presentation.components.GeometryDrawingStep::class.java,
                        ).type
                    return gson.fromJson(json, type) ?: emptyList()
                }
                val fenceStart = text.indexOf("```json")
                if (fenceStart >= 0) {
                    val fenceEnd = text.indexOf("```", fenceStart + 7)
                    if (fenceEnd > fenceStart) {
                        val json = text.substring(fenceStart + 7, fenceEnd).trim()
                        val gson = com.google.gson.Gson()
                        val type =
                            com.google.gson.reflect.TypeToken.getParameterized(
                                List::class.java,
                                com.example.solver.geometry_solver.presentation.components.GeometryDrawingStep::class.java,
                            ).type
                        return gson.fromJson(json, type) ?: emptyList()
                    }
                }
            } catch (_: Exception) {
            }
            return emptyList()
        }

        private fun encodeImage(uri: Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) return null

                // Scale down if too large to prevent OOM and reduce network payload
                val maxDim = 1024
                val scaledBitmap =
                    if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else {
                        bitmap
                    }

                // Use an initial capacity of 1MB to reduce memory reallocation during compression
                val outputStream = ByteArrayOutputStream(1024 * 1024)
                // Try 80% quality first
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

                // Safety check: if the base64 string is still too large, compress further
                var bytes = outputStream.toByteArray()
                var quality = 80
                while (bytes.size > 2 * 1024 * 1024 && quality > 20) { // Limit to ~2MB
                    quality -= 20
                    outputStream.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    bytes = outputStream.toByteArray()
                }

                // Clean up native bitmap memory
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                }
                scaledBitmap.recycle()

                "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
