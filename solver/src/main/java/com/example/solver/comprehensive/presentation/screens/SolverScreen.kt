package com.example.solver.comprehensive.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.common.ui.components.SafeMarkdownText
import com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    viewModel: SolverViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("几何解题", "代数解题", "综合解题")
    var showErrorBookDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
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
            TabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = { Text(title) }
                    )
                }
            }

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

                // Input Area
                OutlinedTextField(
                    value = uiState.questionText,
                    onValueChange = { viewModel.updateQuestionText(it) },
                    label = { Text("输入题目描述 (可选)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 200.dp),
                    maxLines = 10
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSolving
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("相册选图")
                    }
                    
                    // TODO: Could integrate with CameraScreen in the future. For now, picking image is sufficient.
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
                        Text("开始解题")
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
            }
        }
        
        if (showErrorBookDialog) {
            var subject by remember { mutableStateOf(tabs[uiState.selectedTab].replace("解题", "")) }
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
    }
}
