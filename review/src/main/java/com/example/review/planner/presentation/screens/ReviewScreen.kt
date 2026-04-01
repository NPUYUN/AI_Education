package com.example.review.planner.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.common.R
import com.example.common.database.models.ErrorBookEntity
import com.example.common.database.models.ReviewHistoryEntity
import com.example.common.ui.components.SafeMarkdownText
import com.example.review.planner.presentation.viewmodels.ReviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReviewPlannerScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            SmartReviewPlannerView(viewModel, uiState)
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
            onDelete = { viewModel.deleteHistory(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeReinforcementScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            KnowledgeReinforcementView(viewModel, uiState)
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
            onDelete = { viewModel.deleteHistory(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBookScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.error_book)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            ErrorBookView(viewModel, uiState)
        }
    }
}

@Composable
fun HistoryDialog(
    title: String,
    historyList: List<ReviewHistoryEntity>,
    onDismiss: () -> Unit,
    onSelect: (ReviewHistoryEntity) -> Unit,
    onDelete: (ReviewHistoryEntity) -> Unit
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(history) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(history.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onDelete(history) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = history.inputParameters,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
        }
    )
}

@Composable
fun SmartReviewPlannerView(
    viewModel: ReviewViewModel,
    uiState: com.example.review.planner.presentation.viewmodels.ReviewUiState,
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

                    AnimatedVisibility(
                        visible = plan.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(20.dp),
                            ) {
                                SafeMarkdownText(
                                    markdown = plan,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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
) {
    val input = uiState.knowledgePointInput
    val isGenerating = uiState.isGeneratingQuiz
    val quiz = uiState.reinforcementQuiz

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

        AnimatedVisibility(
            visible = quiz.isNotBlank(),
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + shrinkVertically(),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                ) {
                    item {
                        SafeMarkdownText(
                            markdown = quiz,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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

                                Text(
                                    stringResource(R.string.question_colon),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    record.questionContent,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    stringResource(R.string.error_reason_colon),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    record.errorReason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    stringResource(R.string.correct_analysis_colon),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    record.correctSolution,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = stringResource(R.string.recording_time, sdf.format(Date(record.timestamp))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                )
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
                        onClick = { viewModel.startRedoPractice() },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    ) {
                        Text("一键测试")
                    }
                    Button(
                        onClick = { showGenerateDialog = true },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text("一键生成")
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("生成相似题目") },
            text = {
                Column {
                    Text("请选择要生成的题目总数: ${generateCount.toInt()}题")
                    Slider(
                        value = generateCount,
                        onValueChange = { generateCount = it },
                        valueRange = 1f..20f,
                        steps = 19,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    viewModel.generateSimilarPractice(generateCount.toInt())
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("取消")
                }
            },
        )
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("做题与批改", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.closePracticeScreen() }) {
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
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
            if (uiState.isGeneratingPractice) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在生成高质量题目...")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f).shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                    ) {
                        item {
                            SafeMarkdownText(
                                markdown = uiState.practiceContent,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("你的作答：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.practiceAnswerInput,
                                onValueChange = { viewModel.updatePracticeAnswer(it) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                                placeholder = { Text("请在此输入你的答案，按题目顺序作答...") },
                                shape = RoundedCornerShape(12.dp),
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.gradePractice() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isGradingPractice,
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                if (uiState.isGradingPractice) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("正在批改...")
                                } else {
                                    Text("提交批改")
                                }
                            }
                        }

                        item {
                            AnimatedVisibility(
                                visible = uiState.practiceGradingResult.isNotBlank(),
                                enter = fadeIn() + expandVertically(),
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("批改结果", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            SafeMarkdownText(
                                                markdown = uiState.practiceGradingResult,
                                                modifier = Modifier.fillMaxWidth()
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
    }
}
