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
import com.example.common.database.models.ErrorBookEntity
import com.example.solver.comprehensive.services.SolverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class SolverUiState(
    val selectedTab: Int = 0,
    val questionText: String = "",
    val imageUri: Uri? = null,
    val isSolving: Boolean = false,
    val solutionResult: String = "",
    val error: String? = null,
    val isAddedToErrorBook: Boolean = false
)

@HiltViewModel
class SolverViewModel @Inject constructor(
    private val repository: SolverRepository,
    private val globalConfigRepository: GlobalConfigRepository,
    private val errorBookDao: ErrorBookDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SolverUiState())
    val uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(
            selectedTab = index,
            questionText = "",
            imageUri = null,
            solutionResult = "",
            error = null,
            isAddedToErrorBook = false
        )
    }

    fun updateQuestionText(text: String) {
        _uiState.value = _uiState.value.copy(questionText = text, error = null, isAddedToErrorBook = false)
    }

    fun setImageUri(uri: Uri?) {
        _uiState.value = _uiState.value.copy(imageUri = uri, error = null, isAddedToErrorBook = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun addToErrorBook(subject: String, errorReason: String) {
        val state = _uiState.value
        if (state.solutionResult.isBlank()) return
        
        viewModelScope.launch {
            try {
                val questionContent = if (state.questionText.isNotBlank()) state.questionText else "图片题目（暂无文字描述）"
                val entity = ErrorBookEntity(
                    subject = subject,
                    questionContent = questionContent,
                    errorReason = errorReason,
                    correctSolution = state.solutionResult
                )
                errorBookDao.insertErrorRecord(entity)
                _uiState.value = _uiState.value.copy(isAddedToErrorBook = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "保存到错题本失败: ${e.message}")
            }
        }
    }

    fun solveProblem() {
        val state = _uiState.value
        if (state.questionText.isBlank() && state.imageUri == null) {
            _uiState.value = state.copy(error = "请输入题目或上传题目图片")
            return
        }

        _uiState.value = state.copy(isSolving = true, error = null, solutionResult = "")

        viewModelScope.launch {
            try {
                val apiKey = globalConfigRepository.getAiTutorApiKey().firstOrNull()
                    ?.takeIf { it.isNotBlank() } ?: AppConstants.DEFAULT_API_KEY
                val modelName = globalConfigRepository.getAiTutorModelName().firstOrNull()
                    ?: AppConstants.DEFAULT_MODEL_NAME
                val baseUrl = globalConfigRepository.getAiTutorBaseUrl().firstOrNull()
                    ?: AppConstants.BASE_URL

                val systemPrompt = when (state.selectedTab) {
                    0 -> AppConstants.SOLVER_GEOMETRY_SYSTEM_PROMPT
                    1 -> AppConstants.SOLVER_ALGEBRA_SYSTEM_PROMPT
                    else -> AppConstants.SOLVER_COMPREHENSIVE_SYSTEM_PROMPT
                }

                var base64Image: String? = null
                state.imageUri?.let { uri ->
                    base64Image = encodeImage(uri)
                }

                val result = repository.solveProblem(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    modelName = modelName,
                    systemPrompt = systemPrompt,
                    questionText = state.questionText,
                    base64Image = base64Image
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSolving = false,
                        solutionResult = result.getOrNull() ?: ""
                    )
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsg = when (exception) {
                        is SocketTimeoutException -> "网络请求超时，请稍后重试"
                        is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                        else -> exception?.message ?: "未知错误"
                    }
                    _uiState.value = _uiState.value.copy(
                        isSolving = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is SocketTimeoutException -> "网络请求超时，请稍后重试"
                    is UnknownHostException -> "无法连接到服务器，请检查网络设置"
                    else -> e.message ?: "发生异常"
                }
                _uiState.value = _uiState.value.copy(
                    isSolving = false,
                    error = errorMsg
                )
            }
        }
    }

    private fun encodeImage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Scale down if too large to prevent OOM and reduce network payload
            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
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

            "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
