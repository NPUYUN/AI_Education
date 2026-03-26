package com.example.common.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.common.config.AppConstants
import com.example.common.database.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val COMMON_MODELS =
    listOf(
        "qwen-turbo" to "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        "qwen-vl-plus" to "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        "qwen-plus" to "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        "qwen-max" to "https://dashscope.aliyuncs.com/compatible-mode/v1/",
        "deepseek-chat" to "https://api.deepseek.com/v1/",
        "deepseek-reasoner" to "https://api.deepseek.com/v1/",
        "moonshot-v1-8k" to "https://api.moonshot.cn/v1/",
        "moonshot-v1-32k" to "https://api.moonshot.cn/v1/",
        "glm-4" to "https://open.bigmodel.cn/api/paas/v4/",
        "gpt-3.5-turbo" to "https://api.openai.com/v1/",
        "gpt-4o" to "https://api.openai.com/v1/",
        "gpt-4o-mini" to "https://api.openai.com/v1/",
        "claude-3-5-sonnet-20240620" to "https://api.anthropic.com/v1/",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalApiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }

    var useGlobalApi by remember { mutableStateOf(false) }

    // Global Settings
    var globalApiKey by remember { mutableStateOf("") }
    var globalModel by remember { mutableStateOf("") }
    var globalBaseUrl by remember { mutableStateOf("") }

    // AI Tutor
    var tutorApiKey by remember { mutableStateOf("") }
    var tutorModel by remember { mutableStateOf("") }
    var tutorBaseUrl by remember { mutableStateOf("") }

    // Video Summary
    var videoApiKey by remember { mutableStateOf("") }
    var videoModel by remember { mutableStateOf("") }
    var videoBaseUrl by remember { mutableStateOf("") }

    // Timeline Map
    var timelineApiKey by remember { mutableStateOf("") }
    var timelineModel by remember { mutableStateOf("") }
    var timelineBaseUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        prefs.getBoolean("use_global_api", false).collect {
            useGlobalApi = it
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("global_api_key", "").collect {
            globalApiKey = it
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("global_model_name", "").collect {
            globalModel = it.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("global_base_url", "").collect {
            globalBaseUrl = it.ifBlank { AppConstants.BASE_URL }
        }
    }

    LaunchedEffect(Unit) {
        prefs.getString("api_key_ai_tutor", "").collect {
            tutorApiKey = it.ifBlank { AppConstants.DEFAULT_API_KEY }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("model_name_ai_tutor", "").collect {
            tutorModel = it.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("base_url_ai_tutor", "").collect {
            tutorBaseUrl = it.ifBlank { AppConstants.BASE_URL }
        }
    }

    LaunchedEffect(Unit) {
        prefs.getString("api_key_video_summary", "").collect {
            videoApiKey = it.ifBlank { AppConstants.DEFAULT_API_KEY }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("model_name_video_summary", "").collect {
            videoModel = it.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("base_url_video_summary", "").collect {
            videoBaseUrl = it.ifBlank { AppConstants.BASE_URL }
        }
    }

    LaunchedEffect(Unit) {
        prefs.getString("api_key_timeline_map", "").collect {
            timelineApiKey = it.ifBlank { AppConstants.DEFAULT_API_KEY }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("model_name_timeline_map", "").collect {
            timelineModel = it.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
        }
    }
    LaunchedEffect(Unit) {
        prefs.getString("base_url_timeline_map", "").collect {
            timelineBaseUrl = it.ifBlank { AppConstants.BASE_URL }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                Text(
                    text = "全局 API 设置",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "统一使用全局配置",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = useGlobalApi,
                        onCheckedChange = { useGlobalApi = it },
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (useGlobalApi) {
                        ApiSettingSection(
                            title = "全局配置 (所有模块统一使用)",
                            apiKey = globalApiKey,
                            onApiKeyChange = { globalApiKey = it },
                            modelName = globalModel,
                            onModelNameChange = { globalModel = it },
                            baseUrl = globalBaseUrl,
                            onBaseUrlChange = { globalBaseUrl = it },
                        )
                        HorizontalDivider()
                    }

                    ApiSettingSection(
                        title = "AI 辅导",
                        apiKey = if (useGlobalApi) globalApiKey else tutorApiKey,
                        onApiKeyChange = { tutorApiKey = it },
                        modelName = if (useGlobalApi) globalModel else tutorModel,
                        onModelNameChange = { tutorModel = it },
                        baseUrl = if (useGlobalApi) globalBaseUrl else tutorBaseUrl,
                        onBaseUrlChange = { tutorBaseUrl = it },
                        enabled = !useGlobalApi,
                    )

                    HorizontalDivider()

                    ApiSettingSection(
                        title = "视频总结",
                        apiKey = if (useGlobalApi) globalApiKey else videoApiKey,
                        onApiKeyChange = { videoApiKey = it },
                        modelName = if (useGlobalApi) globalModel else videoModel,
                        onModelNameChange = { videoModel = it },
                        baseUrl = if (useGlobalApi) globalBaseUrl else videoBaseUrl,
                        onBaseUrlChange = { videoBaseUrl = it },
                        enabled = !useGlobalApi,
                    )

                    HorizontalDivider()

                    ApiSettingSection(
                        title = "时间轴地图",
                        apiKey = if (useGlobalApi) globalApiKey else timelineApiKey,
                        onApiKeyChange = { timelineApiKey = it },
                        modelName = if (useGlobalApi) globalModel else timelineModel,
                        onModelNameChange = { timelineModel = it },
                        baseUrl = if (useGlobalApi) globalBaseUrl else timelineBaseUrl,
                        onBaseUrlChange = { timelineBaseUrl = it },
                        enabled = !useGlobalApi,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.saveBoolean("use_global_api", useGlobalApi)
                                prefs.saveString("global_api_key", globalApiKey)
                                prefs.saveString("global_model_name", globalModel)
                                prefs.saveString("global_base_url", globalBaseUrl)

                                prefs.saveString("api_key_ai_tutor", tutorApiKey)
                                prefs.saveString("model_name_ai_tutor", tutorModel)
                                prefs.saveString("base_url_ai_tutor", tutorBaseUrl)

                                prefs.saveString("api_key_video_summary", videoApiKey)
                                prefs.saveString("model_name_video_summary", videoModel)
                                prefs.saveString("base_url_video_summary", videoBaseUrl)

                                prefs.saveString("api_key_timeline_map", timelineApiKey)
                                prefs.saveString("model_name_timeline_map", timelineModel)
                                prefs.saveString("base_url_timeline_map", timelineBaseUrl)

                                // Fallback global bailian key
                                val effectiveKeyForFallback = if (useGlobalApi) globalApiKey else tutorApiKey
                                if (effectiveKeyForFallback.isNotBlank()) {
                                    val currentBailian = prefs.getString("bailian_api_key", "").first()
                                    if (currentBailian.isBlank()) {
                                        prefs.saveString("bailian_api_key", effectiveKeyForFallback)
                                    }
                                }
                                onDismiss()
                            }
                        },
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingSection(
    title: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(if (enabled) "API Key" else "API Key (全局)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
        )

        ExposedDropdownMenuBox(
            expanded = if (enabled) expanded else false,
            onExpandedChange = { if (enabled) expanded = !expanded },
        ) {
            OutlinedTextField(
                value = modelName,
                onValueChange = onModelNameChange,
                label = { Text(if (enabled) "模型名称" else "模型名称 (全局)") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                singleLine = true,
                enabled = enabled,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                COMMON_MODELS.forEach { (model, url) ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            onModelNameChange(model)
                            onBaseUrlChange(url)
                            expanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text(if (enabled) "Base URL" else "Base URL (全局)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
        )
    }
}
