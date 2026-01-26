package com.example.ai_tutor.presentation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tutor.data.local.AiTutorDatabase
import com.example.ai_tutor.data.local.entity.ChatSessionEntity
import com.example.ai_tutor.data.local.entity.MessageEntity
import com.example.ai_tutor.data.model.ContentItem
import com.example.ai_tutor.data.model.ImageUrl
import com.example.ai_tutor.data.model.Message
import com.example.ai_tutor.data.repository.QwenRepository
import com.example.ai_tutor.domain.AgentDecisionHub
import com.example.ai_tutor.domain.DialogueContext
import com.example.ai_tutor.domain.MockKnowledgeGraphManager
import com.example.ai_tutor.domain.MultimodalProcessor
import com.example.ai_tutor.domain.ToolsIntegrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AiTutorViewModel(application: Application) : AndroidViewModel(application) {
    // Core Dependencies
    // Note: Replace with a valid DashScope API Key. 
    // The previous key might be expired or invalid if receiving 401.
    private val apiKey = "sk-e6a46e1940de419caf8e5b010954a7e3" 
    private val repository = QwenRepository(apiKey)
    private val knowledgeGraph = MockKnowledgeGraphManager()
    private val toolsIntegrator = ToolsIntegrator()
    private val multimodalProcessor = MultimodalProcessor()
    
    private val agentDecisionHub = AgentDecisionHub(repository, knowledgeGraph, toolsIntegrator)
    private val chatDao = AiTutorDatabase.getDatabase(application).chatDao()
    // Simple user ID for now, in real app would get from AuthViewModel or Preferences
    private val userId = "current_user" 
    
    // Exposed Sessions Flow
    val sessions = chatDao.getSessions(userId)

    // UI State
    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> get() = _messages

    private val _inputText = mutableStateOf("")
    val inputText: State<String> = _inputText

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    val suggestions = listOf(
        "如何制定高效的学习计划?",
        "帮我解释一下量子力学的基本原理",
        "请修改这篇英语作文的语法错误"
    )
    
    // Dialogue Context
    private var context = DialogueContext(sessionId = UUID.randomUUID().toString())

    init {
        // Initialize with a new session
        createNewSession()
    }

    private fun createNewSession() {
        val newSessionId = UUID.randomUUID().toString()
        context = DialogueContext(sessionId = newSessionId)
        _messages.clear()
        
        viewModelScope.launch(Dispatchers.IO) {
             val session = ChatSessionEntity(
                id = newSessionId,
                userId = userId,
                title = "New Chat",
                lastMessage = "",
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertSession(session)
        }
    }
    
    fun loadSession(sessionId: String) {
        context = DialogueContext(sessionId = sessionId)
        _messages.clear()
        context.history.clear()
        
        viewModelScope.launch(Dispatchers.IO) {
            val entities = chatDao.getMessages(sessionId).firstOrNull() ?: emptyList()
            val msgs = entities.map { Message(it.role, it.content) }
            
            withContext(Dispatchers.Main) {
                _messages.addAll(msgs)
                context.history.addAll(msgs)
            }
        }
    }

    private val _inputImage = mutableStateOf<Bitmap?>(null)
    
    // ...
    
    fun onInputChanged(text: String) {
        _inputText.value = text
    }
    
    fun onImageCaptured(bitmap: Bitmap) {
        _inputImage.value = bitmap
        _inputText.value = "[图片已添加] 请输入您的问题..."
    }
    
    // Old sendMessage implementation removed to avoid conflict
    
    private fun encodeImage(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    fun onSuggestionClicked(suggestion: String) {
        onInputChanged(suggestion)
        sendMessage()
    }
    
    fun startNewChat() {
        createNewSession()
        _inputText.value = ""
    }

    // Re-adding the correct sendMessage (Multimodal)
    fun sendMessage() {
        val text = _inputText.value.trim()
        val image = _inputImage.value
        if (text.isEmpty() && image == null) return

        _isLoading.value = true
        
        // Convert bitmap to base64 if exists
        val base64Image = image?.let { encodeImage(it) }
        _inputImage.value = null // Clear after processing
        
        // Construct User Message
        val userContent: Any = if (base64Image != null) {
            listOf<ContentItem>(
                ContentItem(type = "image_url", imageUrl = ImageUrl(url = base64Image)),
                ContentItem(type = "text", text = text)
            )
        } else {
            text
        }
        val userMsg = Message("user", userContent)
        
        _messages.add(userMsg)
        context.history.add(userMsg) // Important: Add to history for context
        _inputText.value = ""

        viewModelScope.launch {
            saveMessageToDb(userMsg) // Save user message
            
            // Note: We pass context.history which now INCLUDES the current message.
            // QwenRepository might append it again if we are not careful.
            // Checking QwenRepository: It appends 'content' to 'history'.
            // So we should pass 'history' excluding the last message?
            // Or rely on QwenRepository to construct the request.
            // QwenRepository.sendMessage(content, history) -> messages = history + content.
            // So we should pass history WITHOUT the current message.
            val historyToSend = context.history.dropLast(1)
            
            repository.sendMessage(text, historyToSend, imageUrl = base64Image).collect { chunk ->
                if (chunk.startsWith("Error:")) {
                    _messages.add(Message("system", chunk))
                } else {
                    _messages.add(Message("assistant", chunk))
                    context.history.add(Message("assistant", chunk))
                    saveMessageToDb(Message("assistant", chunk))
                }
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun saveMessageToDb(msg: Message) {
        val contentStr = when (val c = msg.content) {
            is String -> c
            is List<*> -> {
                // For database, just save the text part or a marker
                val items = c.filterIsInstance<ContentItem>()
                val textPart = items.find { it.type == "text" }?.text ?: ""
                val hasImage = items.any { it.type == "image_url" }
                if (hasImage) "[Image] $textPart" else textPart
            }
            else -> ""
        }
        
        val entity = MessageEntity(
            sessionId = context.sessionId,
            role = msg.role,
            content = contentStr,
            timestamp = System.currentTimeMillis()
        )
        chatDao.addMessageAndUpdateSession(entity)
    }
    
    // Placeholder for Voice/Camera Input Handling
    fun onVoiceInput(text: String) {
        onInputChanged(text)
        sendMessage()
    }

    fun sendImageWithPrompt(uri: Uri, prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Load Bitmap
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // 2. Encode
                    val base64Image = encodeImage(bitmap)
                    
                    // 3. Construct Message
                    val userContent = listOf(
                        ContentItem(type = "image_url", imageUrl = ImageUrl(url = base64Image)),
                        ContentItem(type = "text", text = prompt)
                    )
                    val userMsg = Message("user", userContent)
                    
                    // 4. Update UI & History
                    withContext(Dispatchers.Main) {
                        _messages.add(userMsg)
                        context.history.add(userMsg)
                        _isLoading.value = true
                    }
                    
                    saveMessageToDb(userMsg)
                    
                    // 5. Send to Repository
                    val historyToSend = context.history.dropLast(1)
                    
                    repository.sendMessage(prompt, historyToSend, imageUrl = base64Image).collect { chunk ->
                        withContext(Dispatchers.Main) {
                            if (chunk.startsWith("Error:")) {
                                _messages.add(Message("system", chunk))
                            } else {
                                val assistantMsg = Message("assistant", chunk)
                                _messages.add(assistantMsg)
                                context.history.add(assistantMsg)
                                saveMessageToDb(assistantMsg)
                            }
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _messages.add(Message("system", "Error processing image: ${e.message}"))
                    _isLoading.value = false
                }
            }
        }
    }
}
