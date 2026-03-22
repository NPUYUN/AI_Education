package com.example.review.planner.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
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
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTabIndex) {
                    0 -> SmartReviewPlannerView()
                    1 -> KnowledgeReinforcementView()
                    2 -> ErrorBookView()
                }
            }
        }
    }
}

@Composable
fun SmartReviewPlannerView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "艾宾浩斯智能复习计划", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "根据遗忘曲线自动为您安排最佳复习时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* TODO */ }) {
            Text("生成今日复习任务")
        }
    }
}

@Composable
fun KnowledgeReinforcementView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "薄弱知识点巩固", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "系统已分析您的学习数据，生成针对性练习", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* TODO */ }) {
            Text("开始专项突破")
        }
    }
}

@Composable
fun ErrorBookView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = "智能错题本", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "自动收录解题模块与练习中的错题", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* TODO */ }) {
            Text("查看错题统计")
        }
    }
}
