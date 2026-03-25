package com.example.review.planner.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.common.ui.components.SafeMarkdownText
import com.example.review.planner.presentation.viewmodels.ReviewViewModel
import com.example.common.database.models.ErrorBookEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("智能复习计划", "知识点巩固", "错题本")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能复习", fontWeight = FontWeight.Bold) }
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
                        text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(2.dp, RoundedCornerShape(12.dp)),
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                            initialOffsetX = { fullWidth -> if (targetState > initialState) fullWidth else -fullWidth },
                            animationSpec = tween(300)
                        )).togetherWith(
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                                targetOffsetX = { fullWidth -> if (targetState > initialState) -fullWidth else fullWidth },
                                animationSpec = tween(300)
                            )
                        )
                    },
                    label = "TabAnimation"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> SmartReviewPlannerView(viewModel, uiState.subjectInput, uiState.isGeneratingPlan, uiState.reviewPlan)
                        1 -> KnowledgeReinforcementView(viewModel, uiState.knowledgePointInput, uiState.isGeneratingQuiz, uiState.reinforcementQuiz)
                        2 -> ErrorBookView(viewModel, uiState.errorRecords)
                    }
                }
            }
        }
    }
}

@Composable
fun SmartReviewPlannerView(viewModel: ReviewViewModel, subjectInput: String, isGenerating: Boolean, plan: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = subjectInput,
            onValueChange = { viewModel.updateSubjectInput(it) },
            label = { Text("复习科目 (用逗号分隔)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp)
        )

        AnimatedContent(
            targetState = plan.isBlank() && !isGenerating,
            label = "PlanContentState"
        ) { isEmptyState ->
            if (isEmptyState) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "艾宾浩斯智能复习计划", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "根据遗忘曲线自动为您安排最佳复习时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.generateReviewPlan() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("生成今日复习任务")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { viewModel.generateReviewPlan() },
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在生成计划...")
                        } else {
                            Text("重新生成计划")
                        }
                    }

                    AnimatedVisibility(
                        visible = plan.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp)
                            ) {
                                SafeMarkdownText(markdown = plan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeReinforcementView(viewModel: ReviewViewModel, input: String, isGenerating: Boolean, quiz: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { viewModel.updateKnowledgePointInput(it) },
            label = { Text("输入薄弱知识点 (如：二次函数)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.generateReinforcementQuiz() },
            enabled = !isGenerating && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在生成专项练习...")
            } else {
                Text("开始专项突破")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = quiz.isNotBlank(),
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    SafeMarkdownText(markdown = quiz)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ErrorBookView(viewModel: ReviewViewModel, records: List<ErrorBookEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("错题本统计：共 ${records.size} 题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.addMockErrorRecord() },
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加测试数据", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = records.isEmpty(),
            label = "ErrorBookContent"
        ) { isEmpty ->
            if (isEmpty) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无错题记录", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = record.subject,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteErrorRecord(record) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text("题目：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(record.questionContent, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("错误原因：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text(record.errorReason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("正确解析：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text(record.correctSolution, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text(
                                    text = "收录时间：${sdf.format(Date(record.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
