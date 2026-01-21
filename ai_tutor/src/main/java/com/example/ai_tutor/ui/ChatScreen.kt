package com.example.ai_tutor.ui

import android.Manifest
import android.graphics.Bitmap
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai_tutor.R
import com.example.ai_tutor.agent.TutorAgent
import com.example.ai_tutor.core.multimodal.ImageAnalysisManager
import com.example.ai_tutor.core.multimodal.VoiceInputManager
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch

sealed class ChatMessage {
    data class Text(val content: String, val isUser: Boolean) : ChatMessage()
    data class Image(val bitmap: Bitmap, val isUser: Boolean) : ChatMessage()
}

@Composable
fun ChatScreen() {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val tutorAgent = remember { TutorAgent() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val voiceInputManager = remember { VoiceInputManager(context) }
    val imageAnalysisManager = remember { ImageAnalysisManager(context) }
    var isListening by remember { mutableStateOf(false) }

    val promptTemplate = stringResource(R.string.prompt_analysis_result)

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            // Show image in chat
            messages.add(ChatMessage.Image(bitmap, true))
            
            scope.launch {
                // Run analysis (Classification + OCR)
                val analysisResult = imageAnalysisManager.analyzeImage(bitmap)
                
                // Construct prompt but DO NOT show it in UI
                val prompt = String.format(promptTemplate, analysisResult.classification, analysisResult.ocrText)
                
                val response = tutorAgent.processQuery(prompt)
                messages.add(ChatMessage.Text(response, false))
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            messages.add(ChatMessage.Text("[System] Camera permission denied.", false))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceInputManager.destroy()
            imageAnalysisManager.close()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (!isListening) {
                                isListening = true
                                scope.launch {
                                    voiceInputManager.startListening().collect { result ->
                                        inputText = result
                                        isListening = false
                                    }
                                }
                            } else {
                                isListening = false
                            }
                        }) {
                            Text(if (isListening) "..." else "Mic")
                        }
                    }
                )
                
                // Button Row below TextField
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Button (Left side below text field)
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Take Photo"
                        )
                    }

                    // Send Button (Right side)
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val currentInput = inputText
                                messages.add(ChatMessage.Text(currentInput, true))
                                inputText = ""
                                scope.launch {
                                    val response = tutorAgent.processQuery(currentInput)
                                    messages.add(ChatMessage.Text(response, false))
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.send_button))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = when (message) {
        is ChatMessage.Text -> message.isUser
        is ChatMessage.Image -> message.isUser
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            // User Message: Boxed, Right Aligned
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    when (message) {
                        is ChatMessage.Text -> {
                            Text(text = message.content)
                        }
                        is ChatMessage.Image -> {
                            Image(
                                bitmap = message.bitmap.asImageBitmap(),
                                contentDescription = "User Image",
                                modifier = Modifier.height(200.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            // AI Message: No Box, Left Aligned, Markdown
            Box(modifier = Modifier.widthIn(max = 340.dp).padding(start = 4.dp)) {
                when (message) {
                    is ChatMessage.Text -> {
                        MarkdownText(
                            markdown = message.content,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    is ChatMessage.Image -> {
                        // AI generally doesn't send images back in this context yet, but handling it just in case
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
