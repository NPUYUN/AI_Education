package com.example.summarizer.core.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryMenuScreen(
    onNavigateToVideoSummary: () -> Unit,
    onNavigateToTextSummary: () -> Unit,
    onNavigateToAudioSummary: () -> Unit,
    onNavigateToChatSummary: () -> Unit,
    onNavigateToKnowledgeCards: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能总结") }
            )
        }
    ) { paddingValues ->
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .heightIn(min = screenHeight),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)) + 
                        slideInVertically(animationSpec = tween(durationMillis = 300), initialOffsetY = { it / 2 })
            ) {
                SummaryOptionCard(
                    title = "视频总结",
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "视频总结", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = onNavigateToVideoSummary
                )
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 100)) + 
                        slideInVertically(animationSpec = tween(durationMillis = 300, delayMillis = 100), initialOffsetY = { it / 2 })
            ) {
                SummaryOptionCard(
                    title = "文本总结",
                    icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "文本总结", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = onNavigateToTextSummary
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 200)) + 
                        slideInVertically(animationSpec = tween(durationMillis = 300, delayMillis = 200), initialOffsetY = { it / 2 })
            ) {
                SummaryOptionCard(
                    title = "音频总结",
                    icon = { Icon(Icons.Default.Audiotrack, contentDescription = "音频总结", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = onNavigateToAudioSummary
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 300)) + 
                        slideInVertically(animationSpec = tween(durationMillis = 300, delayMillis = 300), initialOffsetY = { it / 2 })
            ) {
                SummaryOptionCard(
                    title = "对话总结",
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "对话总结", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = onNavigateToChatSummary
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 400)) + 
                        slideInVertically(animationSpec = tween(durationMillis = 300, delayMillis = 400), initialOffsetY = { it / 2 })
            ) {
                SummaryOptionCard(
                    title = "知识卡片管理",
                    icon = { Icon(Icons.Default.Style, contentDescription = "知识卡片管理", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) },
                    onClick = onNavigateToKnowledgeCards
                )
            }
        }
    }
}

@Composable
fun SummaryOptionCard(title: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
