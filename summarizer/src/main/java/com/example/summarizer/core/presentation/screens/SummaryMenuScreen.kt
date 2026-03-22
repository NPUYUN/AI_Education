package com.example.summarizer.core.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryMenuScreen(
    onNavigateToVideoSummary: () -> Unit,
    onNavigateToTextSummary: () -> Unit,
    onNavigateToChatSummary: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能总结") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SummaryOptionCard(
                title = "视频总结",
                icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "视频总结", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToVideoSummary
            )
            
            SummaryOptionCard(
                title = "文本总结",
                icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = "文本总结", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToTextSummary
            )

            SummaryOptionCard(
                title = "对话总结",
                icon = { Icon(Icons.Default.ChatBubble, contentDescription = "对话总结", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
                onClick = onNavigateToChatSummary
            )
        }
    }
}

@Composable
fun SummaryOptionCard(title: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(24.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
    }
}
