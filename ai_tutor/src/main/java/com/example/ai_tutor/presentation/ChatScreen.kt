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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AiTutorViewModel = viewModel(),
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

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

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("新建对话") },
                    selected = false,
                    onClick = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider()
                Text("最近一周", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = if (session.title.isEmpty()) "New Chat" else session.title,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            selected = false,
                            onClick = { 
                                viewModel.loadSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("教育助手", style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        IconButton(onClick = { /* TODO: Share */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            containerColor = Color.White
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
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
                        reverseLayout = false,
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(viewModel.messages) { message ->
                            MessageItem(message)
                            Spacer(modifier = Modifier.height(16.dp))
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
            color = Color.DarkGray,
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
        border = BorderStroke(1.dp, Color.LightGray),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
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
            color = Color(0xFFF0F0F0),
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
                                    error = result.throwable.message ?: "Base64 Load Error"
                                }
                            )
                            .build()
                    } else if (imageUrl.startsWith("file://")) {
                        val path = imageUrl.substringAfter("file://")
                        val file = java.io.File(path)
                        if (!file.exists()) {
                            error = "File not found: $path"
                            null
                        } else {
                            coil.request.ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .listener(
                                    onError = { _, result -> 
                                        error = result.throwable.message ?: "File Load Error"
                                    }
                                )
                                .build()
                        }
                    } else {
                        coil.request.ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .listener(
                                onError = { _, result -> 
                                    error = result.throwable.message ?: "Url Load Error"
                                }
                            )
                            .build()
                    }
                } catch (e: Exception) {
                    error = "Init Error: ${e.message}"
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
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFFFFEBEE)) // Light Red for error
                )
            }
            
            if (error != null) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = "❌ Image Error", color = Color.Red, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(text = error ?: "Unknown", color = Color.Red, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun UserTextBubble(text: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(vertical = 4.dp),
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black
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
                color = Color.Black,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
