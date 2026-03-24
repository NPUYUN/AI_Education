package com.example.review.planner.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("智能复习") }
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

            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (uiState.selectedTab) {
                    0 -> SmartReviewPlannerView(viewModel, uiState.subjectInput, uiState.isGeneratingPlan, uiState.reviewPlan)
                    1 -> KnowledgeReinforcementView(viewModel, uiState.knowledgePointInput, uiState.isGeneratingQuiz, uiState.reinforcementQuiz)
                    2 -> ErrorBookView(viewModel, uiState.errorRecords)
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
            enabled = !isGenerating
        )

        if (plan.isBlank() && !isGenerating) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "艾宾浩斯智能复习计划", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "根据遗忘曲线自动为您安排最佳复习时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { viewModel.generateReviewPlan() }) {
                    Text("生成今日复习任务")
                }
            }
        } else {
            Button(
                onClick = { viewModel.generateReviewPlan() },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在生成计划...")
                } else {
                    Text("重新生成计划")
                }
            }

            if (plan.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        SafeMarkdownText(markdown = plan)
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
            label = { Text("输入薄弱知识点 (如：二次函数、牛顿第二定律)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.generateReinforcementQuiz() },
            enabled = !isGenerating && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
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

        if (quiz.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    SafeMarkdownText(markdown = quiz)
                }
            }
        }
    }
}

@Composable
fun ErrorBookView(viewModel: ReviewViewModel, records: List<ErrorBookEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("错题本统计：共 ${records.size} 题", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { viewModel.addMockErrorRecord() }) {
                Icon(Icons.Default.Add, contentDescription = "添加测试数据")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无错题记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = record.subject,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteErrorRecord(record) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("题目：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(record.questionContent, style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("错误原因：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            Text(record.errorReason, style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("正确解析：", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text(record.correctSolution, style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            Text(
                                text = "收录时间：${sdf.format(Date(record.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
