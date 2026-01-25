package com.example.ai_tutor.ui

import android.app.Activity
import android.content.Intent
import com.example.ai_tutor.ui.CustomCropActivity
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.ai_tutor.R
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import com.example.ai_tutor.core.multimodal.VoiceInputManager
import com.example.ai_tutor.core.multimodal.TextToSpeechManager
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Settings

sealed class ChatMessage {
    data class Text(val content: String, val isUser: Boolean, val isStreaming: Boolean = false) : ChatMessage()
    data class Image(val bitmap: Bitmap, val isUser: Boolean) : ChatMessage()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val voiceInputManager = remember { VoiceInputManager(context) }
    val ttsManager = remember { TextToSpeechManager(context) }
    var isListening by remember { mutableStateOf(false) }
    var lastInputWasVoice by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    // Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Full Screen Image State (History)
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    
    // Preview Image State (New Capture/Pick)
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Camera State
    var isCameraOpen by remember { mutableStateOf(false) }
    
    // Load Bitmap when URI changes
    LaunchedEffect(previewUri) {
        if (previewUri != null) {
            try {
                // Load scaled down bitmap to prevent OOM and UI freeze
                previewBitmap = loadBitmapFromUri(context, previewUri!!, 1080, 1920)
            } catch (e: Exception) {
                e.printStackTrace()
                previewBitmap = null
            }
        } else {
            previewBitmap = null
        }
    }
    
    // Camera Uri
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Custom Crop Launcher
    val customCropLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                     scope.launch {
                         val bitmap = loadBitmapFromUri(context, uri, 1080, 1920)
                         if (bitmap != null) {
                             viewModel.sendMessage("", bitmap)
                             previewUri = null
                         }
                     }
                } catch(e: Exception) { e.printStackTrace() }
            }
        } else {
             previewUri = null
        }
    }

    // Take Picture Launcher (Legacy system camera, kept as fallback or removed if fully replaced)
    // Replaced by CameraScreen, but keeping variable if referenced elsewhere, or just commenting out usage.
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            // Legacy flow
            previewUri = tempCameraUri
        }
    }

    // Permission Launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "语音权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要语音权限才能使用此功能", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Pick Image Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            previewUri = uri
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isCameraOpen = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceInputManager.destroy()
            ttsManager.shutdown()
        }
    }

    // Auto-read AI response if last input was voice
    LaunchedEffect(uiState.isStreaming) {
        if (!uiState.isStreaming && lastInputWasVoice) {
            val lastMessage = uiState.messages.lastOrNull()
            if (lastMessage is ChatMessage.Text && !lastMessage.isUser) {
                ttsManager.speak(lastMessage.content)
            }
        }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.let { if (it is ChatMessage.Text) it.content.length else 0 }) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    if (selectedImage != null) {
        Dialog(onDismissRequest = { selectedImage = null }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = selectedImage!!.asImageBitmap(),
                        contentDescription = "Full Screen Image",
                        modifier = Modifier.fillMaxSize().clickable { selectedImage = null }
                    )
                }
            }
        }
    }

    if (isCameraOpen) {
        Dialog(
            onDismissRequest = { isCameraOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            CameraScreen(
                onClose = { isCameraOpen = false },
                onImageCaptured = { uri ->
                    isCameraOpen = false
                    val intent = Intent(context, CustomCropActivity::class.java).apply {
                        data = uri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    customCropLauncher.launch(intent)
                }
            )
        }
    } else if (previewUri != null) {
        if (previewBitmap != null) {
            ImagePreviewScreen(
                uri = previewUri!!,
                bitmap = previewBitmap!!,
                onCancel = { previewUri = null },
                onCrop = {
                    val intent = Intent(context, CustomCropActivity::class.java).apply {
                        data = previewUri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    customCropLauncher.launch(intent)
                },
                onSend = { text ->
                     viewModel.sendMessage(text, previewBitmap!!)
                     previewUri = null
                }
            )
        } else {
             Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Color.White)
             }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    // Sidebar Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "教育助手",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Button(
                            onClick = { 
                                viewModel.createNewSession()
                                inputText = ""
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("新建对话")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    // Static Menu Items
                    NavigationDrawerItem(
                        label = { Text("我的空间") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(painterResource(android.R.drawable.ic_menu_myplaces), null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("智能体") },
                        selected = false,
                        onClick = { /* TODO */ },
                        icon = { Icon(painterResource(android.R.drawable.ic_menu_compass), null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("最近一周", modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    
                    LazyColumn {
                        items(uiState.sessions) { session ->
                            NavigationDrawerItem(
                                label = { Text(session.title, maxLines = 1) },
                                selected = session.id == uiState.currentSessionId,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                shape = RoundedCornerShape(8.dp)
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
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* Mute toggle */ }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Sound")
                            }
                            IconButton(onClick = { /* Share */ }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                        },
                        modifier = Modifier.height(100.dp)
                    )
                }
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    
                    // Messages or Welcome Screen
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.messages.isEmpty()) {
                            WelcomeScreen(onSuggestionClick = { viewModel.sendMessage(it, null) })
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(uiState.messages) { message ->
                                    MessageBubble(message, onImageClick = { selectedImage = it })
                                }
                            }
                        }
                    }
                    
                    // Input Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Input Box
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    // Voice Button
                                    IconButton(
                                        onClick = {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                if (!isListening) {
                                                    ttsManager.stop() // Stop previous TTS
                                                    isListening = true
                                                    scope.launch {
                                                        voiceInputManager.startListening().collect { result ->
                                                            // Auto-send logic for real-time conversation
                                                            viewModel.sendMessage(result, null)
                                                            lastInputWasVoice = true
                                                        }
                                                        isListening = false
                                                    }
                                                } else {
                                                    isListening = false
                                                }
                                            } else {
                                                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = if(isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // TextField
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (inputText.isEmpty()) {
                                            Text("发消息或按住说话...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterStart))
                                        }
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = inputText,
                                            onValueChange = { inputText = it },
                                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                        )
                                    }
                                    
                                    // Camera Button
                                    IconButton(onClick = {
                                         if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            isCameraOpen = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.ic_camera), contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Send or Plus (Upload)
                                    if (inputText.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                viewModel.sendMessage(inputText, null)
                                                inputText = ""
                                                lastInputWasVoice = false
                                            },
                                            enabled = !uiState.isStreaming
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { pickImageLauncher.launch("image/*") }
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Upload", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
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
fun ImagePreviewScreen(
    uri: Uri,
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onCrop: () -> Unit,
    onSend: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Voice Input State
    var isListening by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val voiceInputManager = remember { VoiceInputManager(context) }
    val scope = rememberCoroutineScope()
    
    // Permission Launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "语音权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要语音权限才能使用此功能", Toast.LENGTH_SHORT).show()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { voiceInputManager.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Preview",
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Row {
                IconButton(onClick = onCrop) {
                    Icon(Icons.Filled.Crop, contentDescription = "Crop", tint = Color.White)
                }
                IconButton(onClick = {
                    saveImageToGallery(context, bitmap)
                }) {
                    Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                }
            }
        }
        
        // Bottom Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            // Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("这是什么", "解答一下", "翻译一下").forEach { chipText ->
                    SuggestionChip(
                        onClick = { onSend(chipText) },
                        label = { Text(chipText, color = Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color.DarkGray.copy(alpha = 0.8f)
                        ),
                        border = null
                    )
                }
            }
            
            // Input Box
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Voice Button
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                if (!isListening) {
                                    isListening = true
                                    scope.launch {
                                        voiceInputManager.startListening().collect { result ->
                                            text = result
                                            isListening = false
                                        }
                                    }
                                } else {
                                    isListening = false
                                }
                            } else {
                                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = if(isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    // TextField
                    Box(modifier = Modifier.weight(1f)) {
                        if (text.isEmpty()) {
                            Text("发消息或按住说话...", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterStart))
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    }
                    
                    // Send Button
                    IconButton(
                        onClick = {
                            onSend(text)
                            text = ""
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎回来，聊聊新话题", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        
        val suggestions = listOf(
            "如何制定高效的学习计划？",
            "帮我解释一下量子力学的基本原理",
            "请修改这篇英语作文的语法错误"
        )
        
        suggestions.forEach { text ->
            Surface(
                onClick = { onSuggestionClick(text) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
            ) {
                Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, onImageClick: (Bitmap) -> Unit) {
    val isUser = when (message) {
        is ChatMessage.Text -> message.isUser
        is ChatMessage.Image -> message.isUser
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            // User Message
            when (message) {
                is ChatMessage.Text -> {
                    // Text: Boxed
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Text(text = message.content)
                        }
                    }
                }
                is ChatMessage.Image -> {
                    // Image: No Border, Clickable
                    Image(
                        bitmap = message.bitmap.asImageBitmap(),
                        contentDescription = "User Image",
                        modifier = Modifier
                            .height(200.dp)
                            .widthIn(max = 300.dp)
                            .clickable { onImageClick(message.bitmap) }
                    )
                }
            }
        } else {
            // AI Message: No Box, Left Aligned, Markdown
            Box(modifier = Modifier.widthIn(max = 340.dp).padding(start = 4.dp)) {
                when (message) {
                    is ChatMessage.Text -> {
                        MarkdownText(
                            markdown = message.content,
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    is ChatMessage.Image -> {
                        Image(
                            bitmap = message.bitmap.asImageBitmap(),
                            contentDescription = "AI Image",
                            modifier = Modifier.height(200.dp).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    var fos: java.io.OutputStream? = null
    var uri: Uri? = null
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    
    try {
        uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            fos = context.contentResolver.openOutputStream(uri)
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
    } finally {
        fos?.close()
    }
}

// Helper function to calculate inSampleSize
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

// Helper to load bitmap
suspend fun loadBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
    var inputStream = context.contentResolver.openInputStream(uri)
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()

    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false

    inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
    inputStream?.close()
    bitmap
}
