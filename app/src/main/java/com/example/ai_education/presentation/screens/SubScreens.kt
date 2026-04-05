package com.example.ai_education.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ai_tutor.timeline_map.presentation.screens.TimelineMapScreen
import com.example.common.R
import com.example.common.database.PreferencesManager
import com.example.common.presentation.components.GlobalApiSettingsDialog
import com.example.summarizer.videosummarizer.presentation.screens.VideoDownloadScreen
import com.example.summarizer.videosummarizer.presentation.viewmodels.VideoDownloadViewModel
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    navController: NavController,
    query: String? = null,
) {
    TimelineMapScreen(
        initialQuery = query,
        onNavigateBack = { navController.popBackStack() },
    )
}

@androidx.media3.common.util.UnstableApi
@Composable
fun VideoSummaryScreen(
    viewModel: VideoDownloadViewModel,
    navController: NavController,
) {
    VideoDownloadScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
    )
}

@Composable
fun TextSummaryScreenWrapper(
    viewModel: com.example.summarizer.text_summarizer.presentation.viewmodels.TextSummaryViewModel,
    navController: NavController,
) {
    com.example.summarizer.text_summarizer.presentation.screens.TextSummaryScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() },
    )
}

@Composable
fun AudioSummarySubScreen(navController: NavController) {
    val viewModel: com.example.summarizer.audio_summarizer.presentation.viewmodels.AudioSummaryViewModel = hiltViewModel()
    com.example.summarizer.audio_summarizer.presentation.screens.AudioSummaryScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
    )
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val themeMode by preferencesManager.getString("theme_mode", "auto").collectAsStateWithLifecycle(initialValue = "auto")
    var showApiSettings by remember { mutableStateOf(false) }

    if (showApiSettings) {
        GlobalApiSettingsDialog(onDismiss = { showApiSettings = false })
    }

    SubScreenScaffold(title = stringResource(R.string.settings), onBack = onBack) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.theme_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val themes =
                listOf(
                    "auto" to stringResource(R.string.follow_system),
                    "light" to stringResource(R.string.light_mode),
                    "dark" to stringResource(R.string.dark_mode),
                )

            themes.forEach { (mode, label) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { preferencesManager.saveString("theme_mode", mode) }
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = {
                            scope.launch { preferencesManager.saveString("theme_mode", mode) }
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.language_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val currentLocale =
                androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags().let {
                    if (it.isEmpty() || it == "und") "zh" else it.split("-")[0]
                }

            val languages =
                listOf(
                    "zh" to stringResource(R.string.language_chinese),
                    "en" to stringResource(R.string.language_english),
                )

            languages.forEach { (langCode, label) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                    androidx.core.os.LocaleListCompat.forLanguageTags(langCode),
                                )
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = currentLocale == langCode,
                        onClick = {
                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                androidx.core.os.LocaleListCompat.forLanguageTags(langCode),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showApiSettings = true }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        stringResource(R.string.global_llm_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.config_api_key_model_base_url),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.version_1_0_0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
