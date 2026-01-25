package com.example.ai_tutor.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tutor.agent.TutorAgent
import com.example.ai_tutor.data.ChatRepository
import com.example.common.database.AppDatabase
import com.example.common.database.ChatSessionEntity
import com.example.common.database.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val sessions: List<ChatSessionEntity> = emptyList(),
    val currentSessionId: Long? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isStreaming: Boolean = false,
    val pendingImage: Bitmap? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private val tutorAgent = TutorAgent()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao())
        
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            repository.getAllSessions().collectLatest { sessions ->
                _uiState.value = _uiState.value.copy(sessions = sessions)
                
                // If no session is selected but sessions exist, maybe select the first one?
                // Or keep it null to show "New Chat" state.
                // For now, if we have sessions and currentSessionId is null, we might want to stay in "New Chat" mode.
            }
        }
    }

    fun createNewSession() {
        _uiState.value = _uiState.value.copy(
            currentSessionId = null,
            messages = emptyList(),
            isStreaming = false
        )
    }

    fun selectSession(sessionId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentSessionId = sessionId, isStreaming = false)
            repository.getMessagesForSession(sessionId).collectLatest { entities ->
                val uiMessages = entities.map { entity ->
                    val imagePath = entity.imagePath
                    if (imagePath != null) {
                        val bitmap = loadImageFromInternalStorage(imagePath)
                        if (bitmap != null) {
                             ChatMessage.Image(bitmap, entity.role == "user")
                        } else {
                             ChatMessage.Text("[Image not found]", entity.role == "user")
                        }
                    } else {
                        ChatMessage.Text(entity.content, entity.role == "user")
                    }
                }
                _uiState.value = _uiState.value.copy(messages = uiMessages)
            }
        }
    }

    fun sendMessage(text: String, image: Bitmap?) {
        viewModelScope.launch {
            val currentSessionId = _uiState.value.currentSessionId ?: run {
                // Create new session with title based on first question
                val title = if (text.isNotBlank()) {
                    if (text.length > 20) text.take(20) + "..." else text
                } else "New Chat"
                repository.createSession(title)
            }

            // Update UI State immediately with new session ID if it was null
            if (_uiState.value.currentSessionId == null) {
                _uiState.value = _uiState.value.copy(currentSessionId = currentSessionId)
            }

            // Save image to storage if present
            val imagePath = image?.let { saveImageToInternalStorage(it) }

            // Add User Message to DB
            repository.addMessage(currentSessionId, "user", text, imagePath)
            
            // Add to UI manually for instant feedback
            val currentMessages = _uiState.value.messages.toMutableList()
            if (image != null) {
                currentMessages.add(ChatMessage.Image(image, true))
            }
            if (text.isNotBlank()) {
                currentMessages.add(ChatMessage.Text(text, true))
            }
            
            // Add Placeholder for AI
            currentMessages.add(ChatMessage.Text("", false, true))
            _uiState.value = _uiState.value.copy(messages = currentMessages, isStreaming = true)

            // Stream Response
            val prompt = if (text.isBlank() && image != null) {
                "请帮我识别并解答这张图片中的内容，请展示你的深度思考过程。"
            } else {
                text
            }

            var fullResponse = ""
            try {
                tutorAgent.processQueryStream(prompt, image).collect { chunk ->
                    fullResponse += chunk
                    
                    // Update the last message (AI placeholder)
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    if (updatedMessages.isNotEmpty() && updatedMessages.last() is ChatMessage.Text) {
                        val lastMsg = updatedMessages.last() as ChatMessage.Text
                        if (!lastMsg.isUser) { // Ensure it's AI message
                            updatedMessages[updatedMessages.lastIndex] = lastMsg.copy(content = fullResponse)
                            _uiState.value = _uiState.value.copy(messages = updatedMessages)
                        }
                    }
                }
                
                // Save AI Response to DB after streaming is done
                repository.addMessage(currentSessionId, "assistant", fullResponse)
                
            } catch (e: Exception) {
                fullResponse += "\n[Error: ${e.message}]"
                repository.addMessage(currentSessionId, "assistant", fullResponse)
            } finally {
                _uiState.value = _uiState.value.copy(isStreaming = false)
                
                // Final update to ensure UI shows non-streaming state
                 val updatedMessages = _uiState.value.messages.toMutableList()
                 if (updatedMessages.isNotEmpty()) {
                     val lastMsg = updatedMessages.last() as ChatMessage.Text
                     if (!lastMsg.isUser) {
                        updatedMessages[updatedMessages.lastIndex] = lastMsg.copy(isStreaming = false)
                        _uiState.value = _uiState.value.copy(messages = updatedMessages)
                     }
                 }
            }
        }
    }
    
    fun setPendingImage(bitmap: Bitmap?) {
        _uiState.value = _uiState.value.copy(pendingImage = bitmap)
    }

    private suspend fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val filename = "img_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(getApplication<Application>().filesDir, filename)
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun loadImageFromInternalStorage(path: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
