package com.example.review.planner.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.R
import com.example.common.database.models.ReviewHistoryEntity
import com.example.common.ui.components.SafeMarkdownText
import com.example.review.planner.presentation.viewmodels.ReviewViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReviewPlannerScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    if (uiState.showPracticeScreen) {
        PracticeScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeResultScreen) {
        PracticeResultScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeHistoryForRecordId != null) {
        PracticeHistoryScreenOverlay(viewModel, uiState)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_review_plan)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            SmartReviewPlannerView(
                viewModel = viewModel,
                uiState = uiState,
                onShowResult = { showResultDialog = true },
            )
        }
    }

    if (showResultDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showResultDialog = false },
            properties =
                androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                ),
        ) {
            Scaffold(
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text(stringResource(R.string.smart_review_plan)) },
                        navigationIcon = {
                            IconButton(onClick = { showResultDialog = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        },
                        actions = {
                            val scope = rememberCoroutineScope()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                scope.launch {
                                    com.example.common.utils.PdfExporter.exportToPdf(
                                        context = context,
                                        title = context.getString(R.string.smart_review_plan),
                                        content = uiState.reviewPlan,
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Export PDF")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    SafeMarkdownText(markdown = uiState.reviewPlan)
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryDialog(
            title = "复习计划历史",
            historyList = uiState.plannerHistory,
            onDismiss = { showHistoryDialog = false },
            onSelect = {
                viewModel.loadPlannerHistory(it)
                showHistoryDialog = false
            },
            onDelete = { viewModel.deleteHistory(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeReinforcementScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    if (uiState.showPracticeScreen) {
        PracticeScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeResultScreen) {
        PracticeResultScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeHistoryForRecordId != null) {
        PracticeHistoryScreenOverlay(viewModel, uiState)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.knowledge_point_consolidation)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            KnowledgeReinforcementView(
                viewModel = viewModel,
                uiState = uiState,
                onShowResult = { showResultDialog = true },
            )
        }
    }

    if (showResultDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showResultDialog = false },
            properties =
                androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                ),
        ) {
            Scaffold(
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text(stringResource(R.string.knowledge_point_consolidation)) },
                        navigationIcon = {
                            IconButton(onClick = { showResultDialog = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        },
                        actions = {
                            val scope = rememberCoroutineScope()
                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(onClick = {
                                scope.launch {
                                    com.example.common.utils.PdfExporter.exportToPdf(
                                        context = context,
                                        title = context.getString(R.string.knowledge_point_consolidation),
                                        content = uiState.reinforcementSummary,
                                    )
                                }
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Export PDF")
                            }
                        },
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    SafeMarkdownText(markdown = uiState.reinforcementSummary)
                    
                    if (uiState.practiceProblems.isNotEmpty() && uiState.practiceSource == "reinforcement") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                showResultDialog = false
                                viewModel.startPractice() 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("开始随堂测试 (共 ${uiState.practiceProblems.size} 题)", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        HistoryDialog(
            title = "知识巩固历史",
            historyList = uiState.reinforcementHistory,
            onDismiss = { showHistoryDialog = false },
            onSelect = {
                viewModel.loadReinforcementHistory(it)
                showHistoryDialog = false
            },
            onDelete = { viewModel.deleteHistory(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBookScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorEvent) {
        errorEvent?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    if (uiState.showPracticeScreen) {
        PracticeScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeResultScreen) {
        PracticeResultScreenOverlay(viewModel, uiState)
        return
    }

    if (uiState.showPracticeHistoryForRecordId != null) {
        PracticeHistoryScreenOverlay(viewModel, uiState)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.error_book)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (uiState.errorRecords.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val fullContent =
                                    buildString {
                                        append("# 错题本\n\n")
                                        uiState.errorRecords.forEachIndexed { index, record ->
                                            append("## ${index + 1}. [${record.subject}]\n\n")
                                            append(record.questionContent)
                                            if (!record.errorReason.isNullOrBlank()) {
                                                append("\n\n**错误原因/知识点：**\n\n")
                                                append(record.errorReason)
                                            }
                                            append("\n\n---\n\n")
                                        }
                                    }
                                com.example.common.utils.PdfExporter.exportToPdf(
                                    context = context,
                                    title = context.getString(R.string.error_book),
                                    content = fullContent,
                                )
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Export PDF")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            ErrorBookView(viewModel, uiState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeHistoryScreenOverlay(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
) {
    BackHandler {
        viewModel.closePracticeHistory()
    }

    val historyList = uiState.practiceHistory.filter { it.type == "practice_${uiState.showPracticeHistoryForRecordId}" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史测试记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closePracticeHistory() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("暂无测试记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(historyList, key = { it.id }) { history ->
                    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState()
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    if (dismissState.currentValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart ||
                        dismissState.currentValue == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                    ) {
                        LaunchedEffect(Unit) {
                            showDeleteConfirm = true
                        }
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = {
                                showDeleteConfirm = false
                            },
                            title = { Text("删除记录") },
                            text = { Text("确定要删除这条测试记录吗？") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    viewModel.deleteHistory(history)
                                }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                }) {
                                    Text("取消")
                                }
                            },
                        )
                    }

                    androidx.compose.material3.SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = MaterialTheme.colorScheme.errorContainer
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                    ) {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadPracticeHistoryDetail(history) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = "测试时间：${sdf.format(Date(history.timestamp))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val typeToken = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                val data: Map<String, Any>? =
                                    try {
                                        com.google.gson.Gson().fromJson(history.resultContent, typeToken)
                                    } catch (
                                        e: Exception,
                                    ) {
                                        null
                                    }

                                if (data != null) {
                                    val total = (data["problems"] as? List<*>)?.size ?: 0
                                    val accuracy = (data["accuracy"] as? Double)?.toInt() ?: 0
                                    val rawScore = (data["totalScore"] as? Double)?.toInt() ?: 0
                                    val score = if (total > 0 && rawScore > 100) {
                                        (rawScore.toFloat() / (total * 100) * 100).toInt()
                                    } else {
                                        rawScore.coerceAtMost(100)
                                    }
                                    Text(
                                        text = "共 $total 题，得分：$score，正确率：$accuracy%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                } else {
                                    Text(
                                        text = "无法解析历史记录摘要",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeResultScreenOverlay(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
) {
    BackHandler {
        viewModel.closePracticeResultScreen()
    }

    val problems = uiState.practiceProblems
    val results = uiState.practiceGradingResults
    if (problems.isEmpty() || results.isEmpty()) return

    val totalQuestions = problems.size
    val correctCount = results.count { it.isCorrect }
    // 强制将总得分归一化到 100 分制
    val totalScore = if (totalQuestions > 0) {
        val rawSum = results.sumOf { it.score }
        val maxRawSum = totalQuestions * 100 // In case AI still outputs 100 per question
        if (maxRawSum > 100 && rawSum > 100) {
            (rawSum.toFloat() / maxRawSum * 100).toInt()
        } else {
            rawSum.coerceAtMost(100)
        }
    } else {
        0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("测试结果", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closePracticeResultScreen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("得分", style = MaterialTheme.typography.bodyMedium)
                        Text("$totalScore", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("正确率", style = MaterialTheme.typography.bodyMedium)
                        val accuracy = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
                        Text("$accuracy%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(totalQuestions) { index ->
                    val problem = problems[index]
                    val result = results.getOrNull(index) ?: return@items
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { expanded = !expanded }
                                .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = if (result.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (result.isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "第 ${index + 1} 题",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SafeMarkdownText(markdown = problem.questionText, modifier = Modifier.fillMaxWidth())

                                    if (!expanded) {
                                        Text(
                                            "点击查看解析",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }

                            if (expanded) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("你的作答：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uiState.practiceAnswers[index] ?: "未作答", color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("标准答案：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                SafeMarkdownText(markdown = problem.answer, modifier = Modifier.fillMaxWidth())

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("AI 批改解析：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                SafeMarkdownText(markdown = result.explanation, modifier = Modifier.fillMaxWidth())

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        viewModel.addGeneratedProblemToErrorBook(problem)
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("加入错题本")
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
fun HistoryDialog(
    title: String,
    historyList: List<ReviewHistoryEntity>,
    onDismiss: () -> Unit,
    onSelect: (ReviewHistoryEntity) -> Unit,
    onDelete: (ReviewHistoryEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("暂无历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(historyList) { history ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelect(history) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(history.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    IconButton(
                                        onClick = { onDelete(history) },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = history.inputParameters,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
fun SmartReviewPlannerView(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
    onShowResult: () -> Unit = {},
) {
    val subjectInput = uiState.subjectInput
    val isGenerating = uiState.isGeneratingPlan
    val plan = uiState.reviewPlan

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        OutlinedTextField(
            value = subjectInput,
            onValueChange = { viewModel.updateSubjectInput(it) },
            label = { Text("输入附加需求或科目（可选）") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable {
                    viewModel.toggleUseRecentContextForPlan(!uiState.useRecentContextForPlan)
                },
        ) {
            Checkbox(
                checked = uiState.useRecentContextForPlan,
                onCheckedChange = { viewModel.toggleUseRecentContextForPlan(it) },
            )
            Text("基于最近学习记录（对话和错题）找出薄弱点生成计划", style = MaterialTheme.typography.bodyMedium)
        }

        AnimatedContent(
            targetState = plan.isBlank() && !isGenerating,
            label = "PlanContentState",
        ) { isEmptyState ->
            if (isEmptyState) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(R.string.ebbinghaus_smart_review_plan),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auto_arrange_review_time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.generateReviewPlan() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.generate_todays_review_tasks))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { viewModel.generateReviewPlan() },
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.generating_plan))
                        } else {
                            Text(stringResource(R.string.regenerate_plan))
                        }
                    }

                    if (plan.isNotBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onShowResult,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("查看复习计划", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeReinforcementView(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
    onShowResult: () -> Unit = {},
) {
    val input = uiState.knowledgePointInput
    val isGenerating = uiState.isGeneratingQuiz
    val summary = uiState.reinforcementSummary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { viewModel.updateKnowledgePointInput(it) },
            label = { Text("输入附加需求或知识点（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp).clickable {
                    viewModel.toggleUseRecentContextForQuiz(!uiState.useRecentContextForQuiz)
                },
        ) {
            Checkbox(
                checked = uiState.useRecentContextForQuiz,
                onCheckedChange = { viewModel.toggleUseRecentContextForQuiz(it) },
            )
            Text("基于最近学习记录总结知识点", style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { viewModel.generateReinforcementQuiz() },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.generating_special_practice))
            } else {
                Text(stringResource(R.string.start_special_breakthrough))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (summary.isNotBlank()) {
            Button(
                onClick = onShowResult,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("查看知识巩固结果", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErrorBookView(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var generateCount by remember { mutableFloatStateOf(5f) }

    val records = uiState.errorRecords
    val selectedIds = uiState.selectedErrorIds

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.error_book_stats_total, records.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row {
                TextButton(onClick = {
                    if (selectedIds.size == records.size && records.isNotEmpty()) {
                        viewModel.clearErrorSelection()
                    } else {
                        viewModel.selectAllErrors()
                    }
                }) {
                    Text(if (selectedIds.size == records.size && records.isNotEmpty()) "取消全选" else "全选")
                }
                IconButton(
                    onClick = { viewModel.addMockErrorRecord() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_test_data),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = records.isEmpty(),
            label = "ErrorBookContent",
            modifier = Modifier.weight(1f),
        ) { isEmpty ->
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_error_records),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(records, key = { it.id }) { record ->
                        val isSelected = selectedIds.contains(record.id)
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                                    .shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.toggleErrorSelection(record.id) }
                                    .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        },
                                ),
                            border =
                                if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    null
                                },
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Select",
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 8.dp),
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Text(
                                                text = record.subject,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteErrorRecord(record) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                var expanded by remember { mutableStateOf(false) }

                                Text(
                                    stringResource(R.string.question_colon),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (!expanded) {
                                    Text(
                                        text = record.questionContent.replace(Regex("\\s+"), " ").take(100) + "...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "点击查看详情",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.clickable { expanded = true }.padding(vertical = 4.dp),
                                    )
                                } else {
                                    SafeMarkdownText(
                                        markdown = record.questionContent,
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        stringResource(R.string.error_reason_colon),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafeMarkdownText(
                                        markdown = record.errorReason,
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        stringResource(R.string.correct_analysis_colon),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafeMarkdownText(
                                        markdown = record.correctSolution,
                                        modifier = Modifier.fillMaxWidth(),
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "收起详情",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.clickable { expanded = false }.padding(vertical = 4.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = stringResource(R.string.recording_time, sdf.format(Date(record.timestamp))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { viewModel.openPracticeHistory(record.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("查看历史复习")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom action bar
        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(
                        onClick = { showGenerateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("一键测试", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("生成测试卷") },
            text = {
                Column {
                    Text("请选择要生成的题目数量：${generateCount.toInt()}道")
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = generateCount,
                        onValueChange = { generateCount = it },
                        valueRange = 1f..10f,
                        steps = 8,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenerateDialog = false
                        viewModel.generatePracticeFromErrors(generateCount.toInt())
                    },
                ) {
                    Text("确认生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (uiState.isGeneratingPractice) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在生成试卷，请稍候...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreenOverlay(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
) {
    BackHandler {
        viewModel.closePracticeScreen()
    }

    val problems = uiState.practiceProblems
    if (problems.isEmpty()) return

    val currentIndex = uiState.currentPracticeIndex
    val currentProblem = problems[currentIndex]
    val total = problems.size

    var showAnswerSheet by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("做题进度 ${currentIndex + 1}/$total", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closePracticeScreen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAnswerSheet = true },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Icon(Icons.Default.GridOn, contentDescription = "答题卡")
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            LinearProgressIndicator(
                progress = { (currentIndex + 1) / total.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            // Question area
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f).shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = currentProblem.questionType ?: "练习题",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        if (currentProblem.difficulty != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = currentProblem.difficulty,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SafeMarkdownText(
                        markdown = currentProblem.questionText,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!currentProblem.options.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        currentProblem.options.forEach { option ->
                            SafeMarkdownText(
                                markdown = option,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("你的作答：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.practiceAnswers[currentIndex] ?: "",
                        onValueChange = { viewModel.updatePracticeAnswer(it) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        placeholder = { Text("请在此输入你的答案...") },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { viewModel.setCurrentPracticeIndex(currentIndex - 1) },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    Text("上一题")
                }

                if (currentIndex < total - 1) {
                    Button(
                        onClick = { viewModel.setCurrentPracticeIndex(currentIndex + 1) },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    ) {
                        Text("下一题")
                    }
                } else {
                    Button(
                        onClick = { showSubmitConfirmDialog = true },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("交卷")
                    }
                }
            }
        }
    }

    if (showAnswerSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { showAnswerSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    "答题卡",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // Grid of questions
                val rows = (total + 4) / 5
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until 5) {
                            val i = r * 5 + c
                            if (i < total) {
                                val isAnswered = !uiState.practiceAnswers[i].isNullOrBlank()
                                Surface(
                                    modifier =
                                        Modifier.size(48.dp).clickable {
                                            viewModel.setCurrentPracticeIndex(i)
                                            showAnswerSheet = false
                                        },
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isAnswered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border =
                                        if (i == currentIndex) {
                                            androidx.compose.foundation.BorderStroke(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                            )
                                        } else {
                                            null
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${i + 1}",
                                            color = if (isAnswered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        showAnswerSheet = false
                        showSubmitConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("交卷")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSubmitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("确认交卷") },
            text = { Text("确定要提交试卷吗？交卷后将进行自动批改。") },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitConfirmDialog = false
                    viewModel.submitPractice()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (uiState.isGradingPractice) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在进行AI批改...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
