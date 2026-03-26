package com.example.ai_tutor.multimodal_chat.presentation.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai_tutor.multimodal_chat.services.DialogueContext
import com.example.ai_tutor.multimodal_chat.services.LlmRepository
import com.example.ai_tutor.multimodal_chat.services.MultimodalProcessor
import com.example.ai_tutor.multimodal_chat.services.ToolsIntegrator
import com.example.ai_tutor.timeline_map.services.MockKnowledgeGraphManager
import com.example.common.config.AppConstants
import com.example.common.config.GlobalConfigRepository
import com.example.common.database.dao.ChatDao
import com.example.common.database.models.ChatSessionEntity
import com.example.common.database.models.MessageEntity
import com.example.common.dispatchers.DispatcherProvider
import com.example.common.manager.VoskVoiceManager
import com.example.common.network.llm.ContentItem
import com.example.common.network.llm.ImageUrl
import com.example.common.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import com.example.common.network.llm.ChatMessage as Message

data class AiTutorUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val showApiSettings: Boolean = false,
    val isVoiceMode: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AiTutorViewModel
    @Inject
    constructor(
        private val application: Application,
        private val globalConfigRepository: GlobalConfigRepository,
        private val chatDao: ChatDao,
        private val voskVoiceManager: VoskVoiceManager,
        private val dispatcherProvider: DispatcherProvider,
        private val repository: LlmRepository,
        private val networkMonitor: NetworkMonitor,
    ) : ViewModel() {
        // Core Dependencies
        private var apiKey = ""
        private var baseUrl = AppConstants.BASE_URL
        private var modelName = AppConstants.DEFAULT_MODEL_NAME
        private val knowledgeGraph = MockKnowledgeGraphManager()
        private val toolsIntegrator = ToolsIntegrator()
        private val multimodalProcessor = MultimodalProcessor()

        // Simple user ID for now, in real app would get from AuthViewModel or Preferences
        private val userId = "current_user"

        // Exposed Sessions Flow
        val sessions = chatDao.getSessions(userId)

        // UI State
        private val _uiState = MutableStateFlow(AiTutorUiState())
        val uiState: StateFlow<AiTutorUiState> = _uiState.asStateFlow()

        fun setApiSettingsVisible(visible: Boolean) {
            _uiState.update { it.copy(showApiSettings = visible) }
        }

        val suggestions =
            listOf(
                "如何制定高效的学习计划?",
                "帮我解释一下量子力学的基本原理",
                "请修改这篇英语作文的语法错误",
            )

        // Dialogue Context
        private var context = DialogueContext(sessionId = UUID.randomUUID().toString())

        init {
            checkNotNull(globalConfigRepository) { "GlobalConfigRepository is not injected" }
            checkNotNull(chatDao) { "ChatDao is not injected" }
            checkNotNull(voskVoiceManager) { "VoskVoiceManager is not injected" }
            checkNotNull(dispatcherProvider) { "DispatcherProvider is not injected" }
            checkNotNull(repository) { "LlmRepository is not injected" }

            // Initialize Vosk Voice Manager
            voskVoiceManager.init(viewModelScope)

            viewModelScope.launch(dispatcherProvider.io) {
                // Load API settings
                globalConfigRepository.getEffectiveAiTutorApiKey().collectLatest { key ->
                    apiKey = key
                    updateRepository()
                }
            }
            viewModelScope.launch(dispatcherProvider.io) {
                globalConfigRepository.getAiTutorBaseUrl().collectLatest { url ->
                    baseUrl = url
                    updateRepository()
                }
            }
            viewModelScope.launch(dispatcherProvider.io) {
                globalConfigRepository.getAiTutorModelName().collectLatest { name ->
                    modelName = name
                    updateRepository()
                }
            }

            // Auto-load last session or create new
            initializeSession()

            viewModelScope.launch {
                voskVoiceManager.voiceState.collectLatest { state ->
                    when (state) {
                        is VoskVoiceManager.VoiceState.Loading -> {
                            // Optional: Show loading status
                        }
                        is VoskVoiceManager.VoiceState.Ready -> {
                            // Ready
                        }
                        is VoskVoiceManager.VoiceState.Listening -> {
                            _uiState.update { it.copy(inputText = "正在听...") }
                        }
                        is VoskVoiceManager.VoiceState.Result -> {
                            if (state.text.isNotEmpty()) {
                                _uiState.update { it.copy(inputText = state.text, isVoiceMode = true) }
                                sendMessage()
                            }
                        }
                        is VoskVoiceManager.VoiceState.Error -> {
                            _uiState.update {
                                it.copy(
                                    messages = it.messages + Message("system", "Voice Error: ${state.error}"),
                                    inputText = "",
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun updateRepository() {
            if (apiKey.isNotBlank()) {
                // apiKey is verified
            }
        }

        private fun initializeSession() {
            viewModelScope.launch(dispatcherProvider.io) {
                val sessionsList = chatDao.getSessions(userId).firstOrNull()
                withContext(dispatcherProvider.main) {
                    if (!sessionsList.isNullOrEmpty()) {
                        // Load the most recent session
                        loadSession(sessionsList.first().id)
                    } else {
                        createNewSession()
                    }
                }
            }
        }

        fun deleteSession(sessionId: String) {
            viewModelScope.launch(dispatcherProvider.io) {
                chatDao.deleteSessionAndMessages(sessionId)
                withContext(dispatcherProvider.main) {
                    if (context.sessionId == sessionId) {
                        // If current session is deleted, reload/create new
                        initializeSession()
                    }
                }
            }
        }

        private fun createNewSession() {
            val newSessionId = UUID.randomUUID().toString()
            context = DialogueContext(sessionId = newSessionId)
            _uiState.update { it.copy(messages = emptyList()) }

            viewModelScope.launch(dispatcherProvider.io) {
                val session =
                    ChatSessionEntity(
                        id = newSessionId,
                        userId = userId,
                        title = "New Chat",
                        lastMessage = "",
                        timestamp = System.currentTimeMillis(),
                    )
                chatDao.insertSession(session)
            }
        }

        fun loadSession(sessionId: String) {
            context = DialogueContext(sessionId = sessionId)
            _uiState.update { it.copy(messages = emptyList()) }
            context.history.clear()

            viewModelScope.launch(dispatcherProvider.io) {
                val entities = chatDao.getMessages(sessionId).firstOrNull() ?: emptyList()
                val msgs = entities.map { Message(it.role, deserializeContent(it.content)) }

                withContext(dispatcherProvider.main) {
                    _uiState.update { it.copy(messages = msgs) }
                    context.history.addAll(msgs)
                }
            }
        }

        private val inputImageState = mutableStateOf<Bitmap?>(null)

        // ...

        fun clearErrorMessage() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun handleError(message: String) {
            _uiState.update { it.copy(errorMessage = message, isLoading = false) }
        }

        fun onInputChanged(text: String) {
            _uiState.update { it.copy(inputText = text, isVoiceMode = false) }
        }

        // Vosk Voice Input Handling
        fun startVoiceRecording() {
            voskVoiceManager.startListening()
        }

        fun stopVoiceRecording() {
            voskVoiceManager.stopListening()
        }

        fun cancelVoiceRecording() {
            voskVoiceManager.stopListening()
        }

        // Deprecated: Placeholder for Voice/Camera Input Handling
        fun onVoiceInput(text: String) {
            _uiState.update { it.copy(inputText = text, isVoiceMode = true) }
            sendMessage()
        }

        fun onImageCaptured(bitmap: Bitmap) {
            inputImageState.value = bitmap
            _uiState.update { it.copy(inputText = "[图片已添加] 请输入您的问题...") }
        }

        // ...

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int,
        ): Int {
            val (height: Int, width: Int) = options.outHeight to options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        private fun getRotationDegrees(uri: Uri): Float {
            try {
                val contentResolver = application.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    // Use standard Android ExifInterface
                    val exif = android.media.ExifInterface(inputStream)
                    val orientation =
                        exif.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL,
                        )
                    return when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0f
        }

        private suspend fun loadScaledBitmap(uri: Uri): Bitmap? =
            withContext(dispatcherProvider.io) {
                try {
                    val contentResolver = application.contentResolver
                    val rotation = getRotationDegrees(uri)

                    var bitmap: Bitmap? = null

                    // Optimization for file URIs
                    if (uri.scheme == "file" && uri.path != null) {
                        val file = java.io.File(uri.path!!)
                        if (file.exists()) {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(file.absolutePath, options)

                            options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                            options.inJustDecodeBounds = false
                            bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                        }
                    }

                    if (bitmap == null) {
                        // 1. Decode bounds only
                        val options =
                            BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream, null, options)
                        }

                        // 2. Calculate sample size
                        options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                        options.inJustDecodeBounds = false

                        // 3. Decode with sample size
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            // Fix: decodeStream returns Bitmap?, we should assign it
                            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        }
                    }

                    // Apply rotation if needed
                    if (bitmap != null && rotation != 0f) {
                        val matrix = android.graphics.Matrix()
                        matrix.postRotate(rotation)
                        val rotatedBitmap =
                            Bitmap.createBitmap(
                                bitmap!!,
                                0,
                                0,
                                bitmap!!.width,
                                bitmap!!.height,
                                matrix,
                                true,
                            )
                        if (rotatedBitmap != bitmap) {
                            bitmap!!.recycle()
                        }
                        return@withContext rotatedBitmap
                    }

                    return@withContext bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

        private fun scaleBitmap(bitmap: Bitmap): Bitmap {
            val maxDimension = 1024
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
                return bitmap
            }

            val ratio = originalWidth.toFloat() / originalHeight.toFloat()
            val newWidth: Int
            val newHeight: Int

            if (originalWidth > originalHeight) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        private fun saveImageToInternalStorage(bitmap: Bitmap): String {
            val filename = "img_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(application.filesDir, "chat_images")
            if (!file.exists()) {
                file.mkdirs()
            }
            val imageFile = java.io.File(file, filename)
            try {
                val stream = java.io.FileOutputStream(imageFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                stream.flush()
                stream.close()
                return imageFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }

        private fun encodeImage(bitmap: Bitmap): String {
            val scaledBitmap = scaleBitmap(bitmap)
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            return "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }

        fun onSuggestionClicked(suggestion: String) {
            onInputChanged(suggestion)
            sendMessage()
        }

        fun startNewChat() {
            createNewSession()
            _uiState.update { it.copy(inputText = "") }
        }

        private suspend fun prepareHistoryForApi(history: List<Message>): List<Message> =
            withContext(dispatcherProvider.io) {
                history.map { msg ->
                    if (msg.content is List<*>) {
                        val newContent =
                            (msg.content as List<*>).map { itemAny ->
                                if (itemAny is ContentItem) {
                                    val itemUrl = itemAny.imageUrl?.url
                                    if (itemAny.type == "image_url" && itemUrl?.startsWith("file://") == true) {
                                        val path = itemUrl.substringAfter("file://")
                                        val file = java.io.File(path)
                                        if (file.exists()) {
                                            try {
                                                val bytes = file.readBytes()
                                                val base64 =
                                                    "data:image/jpeg;base64," +
                                                        android.util.Base64.encodeToString(
                                                            bytes,
                                                            android.util.Base64.NO_WRAP,
                                                        )
                                                itemAny.copy(imageUrl = ImageUrl(url = base64))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                itemAny
                                            }
                                        } else {
                                            itemAny
                                        }
                                    } else {
                                        itemAny
                                    }
                                } else {
                                    itemAny
                                }
                            }
                        msg.copy(content = newContent)
                    } else {
                        msg
                    }
                }
            }

        // Re-adding the correct sendMessage (Multimodal)
        fun sendMessage() {
            val text = _uiState.value.inputText.trim()
            val image = inputImageState.value
            if (text.isEmpty() && image == null) return

            val timelineMatch = Regex("生成(.+)的时间轴(地图|图)?").find(text)
            if (timelineMatch != null && image == null) {
                val topic = timelineMatch.groupValues[1]
                _uiState.update { it.copy(inputText = "") }

                viewModelScope.launch {
                    val dbUserMsg = Message("user", text)
                    context.history.add(dbUserMsg)
                    saveMessageToDb(dbUserMsg)
                    _uiState.update { it.copy(messages = it.messages + dbUserMsg) }

                    val aiText = "[TIMELINE_LINK:$topic]"
                    val dbAiMsg = Message("assistant", aiText)
                    context.history.add(dbAiMsg)
                    saveMessageToDb(dbAiMsg)
                    _uiState.update { it.copy(messages = it.messages + dbAiMsg) }
                }
                return
            }

            if (image != null && !isModelImageSupported(modelName)) {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + Message("system", "当前设置的模型 ($modelName) 可能不支持图片输入，请在设置中更换支持视觉的模型（如 qwen-vl-plus）。"),
                        inputText = "",
                    )
                }
                inputImageState.value = null
                return
            }

            _uiState.update { it.copy(isLoading = true) }

            var base64Image: String? = null
            var displayUrl: String? = null

            if (image != null) {
                // 1. Scale Bitmap
                val scaledBitmap = scaleBitmap(image)
                // 2. Save to Local File (for DB and UI)
                val localPath = saveImageToInternalStorage(scaledBitmap)
                if (localPath.isNotEmpty()) {
                    displayUrl = "file://$localPath" // Use file path for display
                }
                // 3. Encode to Base64 (for API)
                val outputStream = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                val bytes = outputStream.toByteArray()
                base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }

            inputImageState.value = null // Clear after processing

            // Construct User Message
            // Use Base64 for UI (immediate display) to guarantee visibility
            // Use File Path for DB/History (storage efficiency)
            val uiImageUrl = base64Image ?: displayUrl
            val dbImageUrl = displayUrl ?: base64Image

            val uiContent: Any =
                if (uiImageUrl != null) {
                    listOf(
                        ContentItem(type = "image_url", imageUrl = ImageUrl(url = uiImageUrl)),
                        ContentItem(type = "text", text = text),
                    )
                } else {
                    text
                }

            val dbContent: Any =
                if (dbImageUrl != null) {
                    listOf(
                        ContentItem(type = "image_url", imageUrl = ImageUrl(url = dbImageUrl)),
                        ContentItem(type = "text", text = text),
                    )
                } else {
                    text
                }

            val uiMsg = Message("user", uiContent)
            val dbMsg = Message("user", dbContent)

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + uiMsg,
                    inputText = "",
                )
            }
            context.history.add(dbMsg) // Add DB-friendly message to history

            viewModelScope.launch {
                if (!networkMonitor.isConnected.value) {
                    _uiState.update { state ->
                        state.copy(
                            messages =
                                state.messages +
                                    Message(
                                        "system",
                                        "当前处于无网络环境。\n已开启离线降级策略：\n您可以继续查看历史记录、错题本，或使用离线语音功能。大模型问答暂不可用，请连接网络后重试。",
                                    ),
                            isLoading = false,
                        )
                    }
                    return@launch
                }

                if (repository == null) {
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + Message("system", "尚未配置 API Key，请在弹出的设置中进行配置。"),
                            isLoading = false,
                            showApiSettings = true,
                        )
                    }
                    return@launch
                }

                saveMessageToDb(dbMsg) // Save DB-friendly message

                // Note: We pass context.history which now INCLUDES the current message.
                val rawHistory = context.history.dropLast(1)
                val historyToSend = prepareHistoryForApi(rawHistory)

                var isFirstChunk = true
                var currentReply = ""

                repository.sendMessage(
                    apiKey = apiKey,
                    modelName = modelName,
                    prompt = text,
                    history = historyToSend,
                    imageUrl = base64Image,
                    baseUrl = baseUrl,
                ).collect { chunk ->
                    if (chunk.startsWith("Error:")) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages + Message("system", chunk),
                                showApiSettings = if (chunk.contains("API Key 无效或未授权")) true else state.showApiSettings,
                                isLoading = false,
                            )
                        }
                    } else {
                        currentReply += chunk
                        _uiState.update { state ->
                            if (isFirstChunk) {
                                isFirstChunk = false
                                state.copy(
                                    messages = state.messages + Message("assistant", currentReply),
                                    isLoading = false,
                                )
                            } else {
                                val newMessages = state.messages.toMutableList()
                                if (newMessages.isNotEmpty() && newMessages.last().role == "assistant") {
                                    newMessages[newMessages.lastIndex] = Message("assistant", currentReply)
                                } else {
                                    newMessages.add(Message("assistant", currentReply))
                                }
                                state.copy(
                                    messages = newMessages,
                                    isLoading = false,
                                )
                            }
                        }
                    }
                }

                if (currentReply.isNotEmpty()) {
                    context.history.add(Message("assistant", currentReply))
                    saveMessageToDb(Message("assistant", currentReply))
                }
            }
        }

        private suspend fun saveMessageToDb(msg: Message) {
            val contentStr = serializeContent(msg.content)

            // Update Title if it's the first user message
            if (msg.role == "user") {
                val userMsgCount = context.history.count { it.role == "user" }
                if (userMsgCount == 1) {
                    val titleText =
                        when (val c = msg.content) {
                            is String -> c.take(15)
                            is List<*> -> (c.find { (it as? ContentItem)?.type == "text" } as? ContentItem)?.text?.take(15) ?: "Image Chat"
                            else -> "Chat"
                        }
                    if (titleText.isNotBlank()) {
                        chatDao.updateSessionTitle(context.sessionId, titleText)
                    }
                }
            }

            val entity =
                MessageEntity(
                    sessionId = context.sessionId,
                    role = msg.role ?: "assistant",
                    content = contentStr,
                    timestamp = System.currentTimeMillis(),
                )
            chatDao.insertMessage(entity)
            chatDao.updateSessionPreview(entity.sessionId, entity.content, entity.timestamp)
        }

        // Placeholder for Voice/Camera Input Handling
        // Removed duplicate onVoiceInput (replaced above)

        private fun isModelImageSupported(modelName: String): Boolean {
            val lowerName = modelName.lowercase()
            return lowerName.contains("vl") ||
                lowerName.contains("gpt-4o") ||
                lowerName.contains("vision") ||
                lowerName.contains("glm-4v") ||
                lowerName.contains("claude-3") ||
                lowerName.contains("gemini")
        }

        fun sendImageWithPrompt(
            uri: Uri,
            prompt: String,
        ) {
            if (!isModelImageSupported(modelName)) {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + Message("system", "当前设置的模型 ($modelName) 可能不支持图片输入，请在设置中更换支持视觉的模型（如 qwen-vl-plus）。"),
                    )
                }
                return
            }

            // 1. Immediate UI Update using raw URI (Fastest)
            val uiImageUrl = uri.toString()
            val uiContent =
                listOf(
                    ContentItem(type = "image_url", imageUrl = ImageUrl(url = uiImageUrl)),
                    ContentItem(type = "text", text = prompt),
                )
            val uiMsg = Message("user", uiContent)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + uiMsg,
                    inputText = "",
                    isLoading = true,
                )
            }

            viewModelScope.launch(dispatcherProvider.io) {
                try {
                    // 2. Heavy Processing (Background)
                    val bitmap = loadScaledBitmap(uri)

                    if (bitmap != null) {
                        // Save to Local (for DB)
                        val localPath = saveImageToInternalStorage(bitmap)
                        val displayUrl = if (localPath.isNotEmpty()) "file://$localPath" else null

                        // Encode (for API)
                        val outputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                        val bytes = outputStream.toByteArray()
                        val base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                        val stableUiImageUrl = displayUrl ?: base64Image

                        if (repository == null) {
                            withContext(dispatcherProvider.main) {
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages + Message("system", "尚未配置 API Key，请在弹出的设置中进行配置。"),
                                        isLoading = false,
                                        showApiSettings = true,
                                    )
                                }
                            }
                            return@launch
                        }

                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                val newMessages = state.messages.toMutableList()
                                val index = newMessages.indexOf(uiMsg)
                                if (index != -1) {
                                    newMessages[index] =
                                        uiMsg.copy(
                                            content =
                                                listOf(
                                                    ContentItem(type = "image_url", imageUrl = ImageUrl(url = stableUiImageUrl)),
                                                    ContentItem(type = "text", text = prompt),
                                                ),
                                        )
                                }
                                state.copy(messages = newMessages)
                            }
                        }

                        // 3. Construct DB/History Message
                        val dbImageUrl: String = displayUrl ?: base64Image

                        val dbContent =
                            listOf(
                                ContentItem(type = "image_url", imageUrl = ImageUrl(url = dbImageUrl)),
                                ContentItem(type = "text", text = prompt),
                            )

                        val dbMsg = Message("user", dbContent)

                        // Update History & DB
                        context.history.add(dbMsg)
                        saveMessageToDb(dbMsg)

                        // 4. Send to Repository
                        val rawHistory = context.history.dropLast(1)
                        val historyToSend = prepareHistoryForApi(rawHistory)

                        var isFirstChunk = true
                        var currentReply = ""

                        repository.sendMessage(
                            apiKey = apiKey,
                            modelName = modelName,
                            prompt = prompt,
                            history = historyToSend,
                            imageUrl = base64Image,
                            baseUrl = baseUrl,
                        ).collect { chunk ->
                            withContext(dispatcherProvider.main) {
                                if (chunk.startsWith("Error:")) {
                                    _uiState.update { state ->
                                        state.copy(
                                            messages = state.messages + Message("system", chunk),
                                            showApiSettings = if (chunk.contains("API Key 无效或未授权")) true else state.showApiSettings,
                                            isLoading = false,
                                        )
                                    }
                                } else {
                                    currentReply += chunk
                                    _uiState.update { state ->
                                        if (isFirstChunk) {
                                            isFirstChunk = false
                                            state.copy(
                                                messages = state.messages + Message("assistant", currentReply),
                                                isLoading = false,
                                            )
                                        } else {
                                            val newMessages = state.messages.toMutableList()
                                            if (newMessages.isNotEmpty() && newMessages.last().role == "assistant") {
                                                newMessages[newMessages.lastIndex] = Message("assistant", currentReply)
                                            } else {
                                                newMessages.add(Message("assistant", currentReply))
                                            }
                                            state.copy(
                                                messages = newMessages,
                                                isLoading = false,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (currentReply.isNotEmpty()) {
                            context.history.add(Message("assistant", currentReply))
                            saveMessageToDb(Message("assistant", currentReply))
                        }
                    } else {
                        withContext(dispatcherProvider.main) {
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages + Message("system", "无法处理图片。"),
                                    isLoading = false,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    handleError("Network Error: ${e.message ?: "Unknown error occurred"}")
                }
            }
        }

        private fun serializeContent(content: Any?): String {
            if (content == null) return ""
            return when (content) {
                is String -> content
                is List<*> -> {
                    val items = content.filterIsInstance<ContentItem>()
                    val jsonArray = org.json.JSONArray()
                    items.forEach { item ->
                        val jsonObj = org.json.JSONObject()
                        jsonObj.put("type", item.type)
                        item.text?.let { jsonObj.put("text", it) }
                        item.imageUrl?.let {
                            val urlObj = org.json.JSONObject()
                            urlObj.put("url", it.url)
                            jsonObj.put("imageUrl", urlObj)
                        }
                        jsonArray.put(jsonObj)
                    }
                    "JSON_CONTENT:$jsonArray"
                }
                else -> content.toString()
            }
        }

        private fun deserializeContent(contentStr: String): Any {
            if (contentStr.startsWith("JSON_CONTENT:")) {
                try {
                    val jsonStr = contentStr.substring("JSON_CONTENT:".length)
                    val jsonArray = org.json.JSONArray(jsonStr)
                    val items = mutableListOf<ContentItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val type = obj.optString("type")
                        val text = if (obj.has("text")) obj.getString("text") else null
                        val imageUrlObj = obj.optJSONObject("imageUrl")
                        val imageUrl =
                            if (imageUrlObj != null) {
                                ImageUrl(url = imageUrlObj.optString("url"))
                            } else {
                                null
                            }
                        items.add(ContentItem(type = type, text = text, imageUrl = imageUrl))
                    }
                    return items
                } catch (e: Exception) {
                    e.printStackTrace()
                    return contentStr
                }
            }
            return contentStr
        }
    }
