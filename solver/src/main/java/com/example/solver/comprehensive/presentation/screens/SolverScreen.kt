package com.example.solver.comprehensive.presentation.screens

import androidx.compose.material3.HorizontalDivider
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.common.ui.components.SafeMarkdownText
import com.example.common.utils.DateFormatUtils
import com.example.common.database.models.SolveHistoryEntity
import com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel
import com.example.solver.geometry_solver.presentation.components.GeometryStepCard
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    viewModel: SolverViewModel,
    onCameraClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showErrorBookDialog by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var selectedHistory: SolveHistoryEntity? by remember { mutableStateOf(null) }
    val recentHistory by viewModel.recentHistory.collectAsState(initial = emptyList())
    val allHistory by viewModel.allHistory.collectAsState(initial = emptyList())

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            viewModel.setImageUri(uriContent)
            if (uriContent != null) {
                viewModel.solveProblem()
            }
        } else {
            val exception = result.error
            Toast.makeText(context, "图片裁剪失败", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val cropOptions = CropImageContractOptions(
                uri = uri,
                cropImageOptions = CropImageOptions(
                    imageSourceIncludeCamera = false,
                    imageSourceIncludeGallery = false,
                    guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
                )
            )
            cropImageLauncher.launch(cropOptions)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onCameraClick()
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
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
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.error ?: "",
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = {
                            if (hasCameraPermission) {
                                onCameraClick()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "拍照解题", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("拍照解题", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier.weight(1f).height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "上传题目", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("上传题目", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = uiState.questionText,
                    onValueChange = { 
                        viewModel.updateQuestionText(it)
                    },
                    label = { Text("题目补充描述（可选）") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 160.dp),
                    maxLines = 8
                )

                // Image Preview
                if (uiState.imageUri != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            AsyncImage(
                                model = uiState.imageUri,
                                contentDescription = "题目图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.setImageUri(null) }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("移除图片")
                            }
                        }
                    }
                }

                Button(
                    onClick = { viewModel.solveProblem() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
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

                AnimatedVisibility(
                    visible = uiState.solutionResult.isNotBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
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
                            if (uiState.selectedTab == 0 && uiState.drawingSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("几何绘图", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                val prev = mutableListOf<Map<String, Any>>()
                                uiState.drawingSteps.forEach { step ->
                                    GeometryStepCard(step, prevShapes = prev.toList(), modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp))
                                    prev += step.shapes
                                }
                            } else if (uiState.selectedTab == 1 && uiState.isFunction && uiState.drawingSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("函数绘图", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                val prev = mutableListOf<Map<String, Any>>()
                                uiState.drawingSteps.forEach { step ->
                                    GeometryStepCard(step, prevShapes = prev.toList(), modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp))
                                    prev += step.shapes
                                }
                            } else if (uiState.selectedTab == 2 && uiState.drawingSteps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val title = when (uiState.comprehensiveType) {
                                    "物理" -> "物理示意图"
                                    "化学" -> "化学示意图"
                                    "生物" -> "生物示意图"
                                    else -> "综合示意图"
                                }
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                val prev = mutableListOf<Map<String, Any>>()
                                uiState.drawingSteps.forEach { step ->
                                    GeometryStepCard(step, prevShapes = prev.toList(), modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp))
                                    prev += step.shapes
                                }
                            }
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentHistory.forEach { item ->
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
                
                AnimatedVisibility(
                    visible = uiState.solutionResult.isBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allHistory.forEach { item ->
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
