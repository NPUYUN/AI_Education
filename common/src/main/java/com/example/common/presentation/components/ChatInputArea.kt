package com.example.common.presentation.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.common.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    onVoiceStart: () -> Unit,
    onVoiceEnd: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRecording by remember { mutableStateOf(false) }

    // Permission for voice
    val context = LocalContext.current
    // Pre-fetch strings for use in callbacks
    val permissionGrantedMsg = stringResource(R.string.permission_granted_press_to_speak)
    val micPermissionReqMsg = stringResource(R.string.microphone_permission_required)
    val holdToSpeakMsg = stringResource(R.string.please_hold_to_speak)
    val inputChatContentMsg = stringResource(R.string.input_chat_content)

    val voicePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                android.widget.Toast.makeText(context, permissionGrantedMsg, android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, micPermissionReqMsg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Voice Button with Hold-to-Talk (Custom implementation replacing IconButton)
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    android.widget.Toast.makeText(context, holdToSpeakMsg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onPress = {
                                    // Check permission logic
                                    val hasPermission =
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (!hasPermission) {
                                        voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@detectTapGestures
                                    }

                                    try {
                                        isRecording = true
                                        onVoiceStart()
                                        // Minimum hold time to prevent instant start/stop errors
                                        val startTime = System.currentTimeMillis()
                                        awaitRelease()
                                        val duration = System.currentTimeMillis() - startTime
                                        if (duration < 500) {
                                            // If too short, maybe just a long tap, but we should delay stop a bit?
                                            // Or rely on user holding it longer.
                                            // Let's just ensure we don't stop TOO fast if the engine needs time.
                                        }
                                    } finally {
                                        isRecording = false
                                        onVoiceEnd()
                                    }
                                },
                            )
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription =
                        if (isRecording) {
                            stringResource(
                                R.string.recording,
                            )
                        } else {
                            stringResource(R.string.long_press_to_speak)
                        },
                    tint = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            TextField(
                value = if (isRecording) stringResource(R.string.release_to_end) else text,
                onValueChange = { if (!isRecording) onTextChanged(it) },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = inputChatContentMsg
                        },
                placeholder = {
                    Text(
                        stringResource(R.string.send_message_or_hold_to_speak),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                singleLine = false,
                maxLines = 4,
                enabled = !isRecording,
            )

            if (text.isNotEmpty() && !isRecording) {
                IconButton(onClick = onSend, enabled = !isLoading) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_message),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (!isRecording) {
                IconButton(onClick = onCameraClick) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.take_photo),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onGalleryClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.select_image),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
    if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
    }
}
