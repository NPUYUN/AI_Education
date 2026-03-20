package com.example.ai_tutor.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ai_tutor.data.model.Message
import com.example.ai_tutor.data.model.ContentItem
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import android.graphics.ImageDecoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.common.presentation.components.ChatInputArea
import com.example.common.presentation.components.GlobalApiSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AiTutorViewModel = hiltViewModel(),
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Removed VoiceInputManager (using Native Recorder in ViewModel)

    // Launchers for Image
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onImageCaptured(bitmap)
            viewModel.sendMessage()
        }
    }

    // Permission Launcher for Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onCameraClick()
        } else {
            // Handle permission denied
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Use sendImageWithPrompt to handle loading, rotation (EXIF), and sending
            viewModel.sendImageWithPrompt(uri, viewModel.inputText.value)
        }
    }

    if (viewModel.showApiSettings.value) {
        GlobalApiSettingsDialog(
            onDismiss = { viewModel.setApiSettingsVisible(false) }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (viewModel.messages.isEmpty()) {
            WelcomeScreen(
                suggestions = viewModel.suggestions,
                onSuggestionClick = { viewModel.onSuggestionClicked(it) },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                        reverseLayout = false,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.messages) { message ->
                            MessageItem(message)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    LaunchedEffect(viewModel.messages.size) {
                        if (viewModel.messages.isNotEmpty()) {
                            listState.animateScrollToItem(viewModel.messages.size - 1)
                        }
                    }
                }

                ChatInputArea(
                    text = viewModel.inputText.value,
                    onTextChanged = { viewModel.onInputChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    isLoading = viewModel.isLoading.value,
                    onVoiceStart = {
                        viewModel.startVoiceRecording()
                    },
                    onVoiceEnd = {
                        viewModel.stopVoiceRecording()
                    },
                    onCameraClick = { 
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    onGalleryClick = { galleryLauncher.launch("image/*") }
                )
            }
        }

@Composable
fun WelcomeScreen(suggestions: List<String>, onSuggestionClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "欢迎回来，聊聊新话题",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        suggestions.forEach { suggestion ->
            SuggestionChip(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun MessageItem(message: Message) {
    val isUser = message.role == "user"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        when (val content = message.content) {
            is String -> {
                if (isUser) {
                    UserTextBubble(content)
                } else {
                    AssistantMessage(content)
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
    
    Column {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 1.dp
        ) {
            val context = LocalContext.current
            val imageRequest = remember(imageUrl) {
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
                                }
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
                                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .wrapContentHeight(),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.errorContainer)
                )
            }
            
            if (error != null) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "❌ Image Error", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(vertical = 4.dp),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun AssistantMessage(markdown: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.Transparent)
    ) {
        MarkdownText(
            markdown = markdown,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
