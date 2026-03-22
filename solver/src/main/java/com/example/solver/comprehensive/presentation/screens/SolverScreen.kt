package com.example.solver.comprehensive.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("几何解题", "代数解题", "综合解题")

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
                    0 -> GeometrySolverView()
                    1 -> AlgebraSolverView()
                    2 -> ComprehensiveSolverView()
                }
            }
        }
    }
}

@Composable
fun GeometrySolverView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "几何解题功能开发中...", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Launch Camera for OCR/Geometry Recognition */ }) {
            Text("拍摄几何题")
        }
    }
}

@Composable
fun AlgebraSolverView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "代数解题功能开发中...", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Launch Camera or Input for Algebra */ }) {
            Text("输入代数题")
        }
    }
}

@Composable
fun ComprehensiveSolverView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "综合学科解题开发中...", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Launch multi-modal solver */ }) {
            Text("AI 综合解答")
        }
    }
}
