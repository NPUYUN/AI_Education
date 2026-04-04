package com.example.ai_tutor.multimodal_chat.presentation.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ai_tutor.multimodal_chat.presentation.viewmodels.AiTutorViewModel
import com.example.common.R
import com.example.common.network.llm.ContentItem
import com.example.common.presentation.components.ChatInputArea
import com.example.common.presentation.components.GlobalApiSettingsDialog
import com.example.common.ui.components.SafeMarkdownText
import kotlinx.coroutines.launch
import com.example.common.network.llm.ChatMessage as Message

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AiTutorViewModel = hiltViewModel(),
    onCameraClick: () -> Unit = {},
    onNavigateToTimeline: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(errorEvent) {
        errorEvent?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    // Removed VoiceInputManager (using Native Recorder in ViewModel)

    // Launchers for Image
    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview(),
        ) { bitmap ->
            if (bitmap != null) {
                viewModel.onImageCaptured(bitmap)
                viewModel.sendMessage()
            }
        }

    // Permission Launcher for Camera
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                onCameraClick()
            } else {
                // Handle permission denied
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                // Use sendImageWithPrompt to handle loading, rotation (EXIF), and sending
                viewModel.sendImageWithPrompt(uri, uiState.inputText)
            }
        }

    if (uiState.showApiSettings) {
        GlobalApiSettingsDialog(
            onDismiss = { viewModel.setApiSettingsVisible(false) },
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (uiState.messages.isEmpty()) {
            WelcomeScreen(
                suggestions = viewModel.suggestions,
                onSuggestionClick = { viewModel.onSuggestionClicked(it) },
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = false,
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(uiState.messages) { message ->
                        var isVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(message) { isVisible = true }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 2 },
                        ) {
                            MessageItem(message, onNavigateToTimeline)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (uiState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }

                LaunchedEffect(uiState.messages.size) {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.size - 1)
                    }
                }

                val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
                LaunchedEffect(imeBottom) {
                    if (imeBottom > 0 && uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.size - 1)
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                )
            }
        }

        ChatInputArea(
            text = uiState.inputText,
            onTextChanged = { viewModel.onInputChanged(it) },
            onSend = {
                viewModel.sendMessage()
            },
            isLoading = uiState.isLoading,
            onVoiceStart = {
                viewModel.startVoiceRecording()
            },
            onVoiceEnd = {
                viewModel.stopVoiceRecording()
            },
            onCameraClick = {
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGalleryClick = { galleryLauncher.launch("image/*") },
        )
    }
}

@Composable
fun WelcomeScreen(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubble,
            contentDescription = null,
            modifier =
                Modifier
                    .size(64.dp)
                    .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.welcome_back_new_topic),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        suggestions.forEach { suggestionKey ->
            val suggestionText = when(suggestionKey) {
                "suggestion_plan" -> stringResource(R.string.ai_tutor_suggestion_plan)
                "suggestion_physics" -> stringResource(R.string.ai_tutor_suggestion_physics)
                "suggestion_english" -> stringResource(R.string.ai_tutor_suggestion_english)
                else -> ""
            }
            if (suggestionText.isNotBlank()) {
                SuggestionChip(
                    text = suggestionText,
                    onClick = { onSuggestionClick(suggestionText) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AssistantTimelineBubble(
    topic: String,
    onNavigateToTimeline: (String) -> Unit,
) {
    val contentDesc = stringResource(R.string.ai_generated_timeline_map_for_topic, topic)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
        modifier =
            Modifier
                .widthIn(max = 320.dp)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                },
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.timeline_map_ready_for_topic, topic),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onNavigateToTimeline(topic) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.view_details))
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    onNavigateToTimeline: (String) -> Unit = {},
) {
    val isUser = message.role == "user"

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        when (val content = message.content) {
            is String -> {
                if (isUser) {
                    UserTextBubble(content)
                } else {
                    if (content.startsWith("[TIMELINE_LINK:")) {
                        val topic = content.substringAfter("[TIMELINE_LINK:").substringBefore("]")
                        AssistantTimelineBubble(topic, onNavigateToTimeline)
                    } else {
                        AssistantMessage(content)
                    }
                }
            }
            is List<*> -> {
                val items = content.filterIsInstance<ContentItem>()
                items.forEach { item ->
                    when (item.type) {
                        "image_url" -> {
                            item.imageUrl?.url?.let { base64Url ->
                                UserImageBubble(base64Url)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        "text" -> {
                            item.text?.let { text ->
                                if (text.isNotEmpty()) {
                                    if (isUser) {
                                        UserTextBubble(text)
                                    } else {
                                        AssistantMessage(text)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserImageBubble(imageUrl: String) {
    var error by remember { mutableStateOf<String?>(null) }

    val imageISentMsg = stringResource(R.string.image_i_sent)
    Column {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .padding(vertical = 4.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = imageISentMsg
                    },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 1.dp,
        ) {
            val context = LocalContext.current
            val imageRequest =
                remember(imageUrl) {
                    try {
                        if (imageUrl.startsWith("data:image")) {
                            val base64Str = imageUrl.substringAfter(",")
                            val imageBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                            coil.request.ImageRequest.Builder(context)
                                .data(imageBytes)
                                .crossfade(true)
                                .listener(
                                    onError = { _, result ->
                                        error = "Base64: ${result.throwable.message}"
                                    },
                                )
                                .build()
                        } else {
                            // Let Coil handle file://, content://, https:// automatically
                            // It handles file permissions and path parsing better than manual File()
                            coil.request.ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .listener(
                                    onError = { _, result ->
                                        error = "Load: ${result.throwable.message}"
                                    },
                                )
                                .build()
                        }
                    } catch (e: Exception) {
                        error = "Init: ${e.message}"
                        null
                    }
                }

            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Uploaded Image",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .wrapContentHeight(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.errorContainer),
                )
            }

            if (error != null) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "❌ Image Error",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(text = error ?: "Unknown", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    // Show truncated path for debugging
                    Text(text = imageUrl.take(100), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, lineHeight = 10.sp)
                }
            }
        }
    }
}

@Composable
fun UserTextBubble(text: String) {
    val contentDesc = stringResource(R.string.message_i_sent, text)
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
        modifier =
            Modifier
                .widthIn(max = 300.dp)
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                },
        shadowElevation = 2.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
fun AssistantMessage(markdown: String) {
    val contentDesc = stringResource(R.string.ai_assistant_reply, markdown)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
        modifier =
            Modifier
                .widthIn(max = 320.dp)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = contentDesc
                },
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            SafeMarkdownText(
                markdown = markdown,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
