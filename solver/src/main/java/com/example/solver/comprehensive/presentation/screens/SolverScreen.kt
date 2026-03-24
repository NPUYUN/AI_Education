package com.example.solver.comprehensive.presentation.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.common.ui.components.SafeMarkdownText
import com.example.common.utils.DateFormatUtils
import com.example.common.database.models.SolveHistoryEntity
import com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    viewModel: SolverViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showErrorBookDialog by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var selectedHistory: SolveHistoryEntity? by remember { mutableStateOf(null) }
    val recentHistory by viewModel.recentHistory.collectAsState(initial = emptyList())
    val allHistory by viewModel.allHistory.collectAsState(initial = emptyList())

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
        if (uri != null) {
            viewModel.solveProblem()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = saveBitmapToMediaStore(context, bitmap)
            viewModel.setImageUri(uri)
            if (uri != null) {
                viewModel.solveProblem()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能解题") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearError() }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除错误", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { cameraLauncher.launch(null) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "拍照解题", modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("拍照解题")
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "上传题目", modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("上传题目")
                        }
                    }
                }
                
                OutlinedTextField(
                    value = uiState.questionText,
                    onValueChange = { 
                        viewModel.updateQuestionText(it)
                    },
                    label = { Text("题目补充描述（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 160.dp),
                    maxLines = 8
                )

                // Image Preview
                if (uiState.imageUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            AsyncImage(
                                model = uiState.imageUri,
                                contentDescription = "题目图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            TextButton(onClick = { viewModel.setImageUri(null) }) {
                                Text("移除图片")
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.solveProblem() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSolving && (uiState.questionText.isNotBlank() || uiState.imageUri != null)
                ) {
                    if (uiState.isSolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在解析中...")
                    } else {
                        Text("开始解题（自动识别题型：${when (uiState.selectedTab) { 0 -> "几何"; 1 -> "代数"; else -> "综合" } }）")
                    }
                }

                if (uiState.solutionResult.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "解题结果",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                            SafeMarkdownText(
                                markdown = uiState.solutionResult,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showErrorBookDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isAddedToErrorBook
                            ) {
                                Text(if (uiState.isAddedToErrorBook) "已收录至错题本" else "收录至错题本")
                            }
                        }
                    }
                }
                
                Text(
                    text = "历史解题",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!showAllHistory) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentHistory) { item ->
                            ElevatedCard(
                                onClick = { selectedHistory = item },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.questionContent.take(30))
                                    }
                                    if (item.isInErrorBook) {
                                        AssistChip(onClick = {}, label = { Text("已入错题本") })
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showAllHistory = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("查看更多")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allHistory) { item ->
                            ElevatedCard(
                                onClick = { selectedHistory = item },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.questionContent)
                                    }
                                    if (item.isInErrorBook) {
                                        AssistChip(onClick = {}, label = { Text("已入错题本") })
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showAllHistory = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("收起")
                            }
                        }
                    }
                }
            }
        }
        
        if (showErrorBookDialog) {
            var subject by remember { mutableStateOf(when (uiState.selectedTab) { 0 -> "几何"; 1 -> "代数"; else -> "综合" }) }
            var errorReason by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showErrorBookDialog = false },
                title = { Text("收录至错题本") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("科目") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = errorReason,
                            onValueChange = { errorReason = it },
                            label = { Text("错误原因/考点") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addToErrorBook(subject, errorReason)
                        showErrorBookDialog = false
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showErrorBookDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
        
        selectedHistory?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedHistory = null },
                title = { Text("解题详情") },
                text = {
                    Column {
                        Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                        Spacer(modifier = Modifier.height(8.dp))
                        if (item.imageUri != null) {
                            AsyncImage(
                                model = item.imageUri,
                                contentDescription = "题目图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text("题目：")
                        Text(item.questionContent)
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("解析：")
                        SafeMarkdownText(
                            markdown = item.solution
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedHistory = null }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}

private fun saveBitmapToMediaStore(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "solver_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
        }
        uri
    } catch (_: Exception) {
        null
    }
}
