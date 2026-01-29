package com.example.ai_tutor.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.Manifest
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

import com.example.ai_tutor.presentation.components.ChatInputArea

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

    // Voice Input Manager
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN") // Force Chinese
        }
    }
    
    // Voice Permission Launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Handled by ChatInputArea
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // Handle error
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Append or replace? Usually append or set
                    viewModel.onInputChanged(matches[0])
                    // Auto-send if needed, but for "Hold to Speak" usually we wait for release.
                    // But onResults comes after end of speech.
                    // If we want auto-send:
                    viewModel.sendMessage()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

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
            scope.launch(Dispatchers.IO) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    withContext(Dispatchers.Main) {
                        viewModel.onImageCaptured(bitmap)
                        viewModel.sendMessage()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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
                Text("我的空间", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                NavigationDrawerItem(
                    label = { Text("智能体") },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider()
                Text("最近一周", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(sessions) { session ->
                        NavigationDrawerItem(
                            label = { Text(if (session.title.isEmpty()) "New Chat" else session.title) },
                            selected = false,
                            onClick = { 
                                viewModel.loadSession(session.id)
                                scope.launch { drawerState.close() }
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
                        try {
                            speechRecognizer.startListening(speechIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onVoiceEnd = {
                        speechRecognizer.stopListening()
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(vertical = 4.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Uploaded Image",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp) // Ensure minimum height to be visible
                .wrapContentHeight()
                .background(Color.Transparent),
            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray),
            error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Red) // More visible error
        )
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
