package com.example.solver.comprehensive.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.common.R
import com.example.common.database.models.SolveHistoryEntity
import com.example.common.ui.components.SafeMarkdownText
import com.example.common.utils.DateFormatUtils
import com.example.solver.comprehensive.presentation.viewmodels.SolverViewModel
import com.example.solver.geometry_solver.presentation.components.GeometryStepCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverScreen(
    viewModel: SolverViewModel,
    onCameraClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showErrorBookDialog by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }
    var selectedHistory: SolveHistoryEntity? by remember { mutableStateOf(null) }
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    var isDetailScreen by remember { mutableStateOf(false) }
    var showSolutionDetailDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(errorEvent) {
        errorEvent?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(uiState.imageUri) {
        if (uiState.imageUri != null) {
            isDetailScreen = true
        }
    }

    val imageCropFailedMsg = stringResource(R.string.image_crop_failed)
    val cropImageLauncher =
        rememberLauncherForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                val uriContent = result.uriContent
                viewModel.setImageUri(uriContent)
                if (uriContent != null) {
                    isDetailScreen = true
                }
            } else {
                val exception = result.error
                Toast.makeText(context, imageCropFailedMsg, Toast.LENGTH_SHORT).show()
            }
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                val cropOptions =
                    CropImageContractOptions(
                        uri = uri,
                        cropImageOptions =
                            CropImageOptions(
                                imageSourceIncludeCamera = false,
                                imageSourceIncludeGallery = false,
                                guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                            ),
                    )
                cropImageLauncher.launch(cropOptions)
            }
        }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val cameraPermissionReqMsg = stringResource(R.string.camera_permission_required_for_photo)
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) {
                onCameraClick()
            } else {
                Toast.makeText(context, cameraPermissionReqMsg, Toast.LENGTH_SHORT).show()
            }
        }

    BackHandler(enabled = isDetailScreen) {
        isDetailScreen = false
        viewModel.setImageUri(null)
        viewModel.updateQuestionText("")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isDetailScreen) {
                            stringResource(
                                R.string.smart_problem_solving,
                            )
                        } else {
                            stringResource(R.string.smart_problem_solving)
                        },
                    )
                },
                navigationIcon = {
                    if (isDetailScreen) {
                        IconButton(onClick = {
                            isDetailScreen = false
                            viewModel.setImageUri(null)
                            viewModel.updateQuestionText("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (!isDetailScreen) {
                // Main Screen: Photo, Upload, History
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                            },
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = stringResource(R.string.photo_problem_solving),
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.photo_problem_solving),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier.weight(1f).height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            onClick = { imagePickerLauncher.launch("image/*") },
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = stringResource(R.string.upload_question),
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.upload_question),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.historical_problem_solving),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!showAllHistory) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recentHistory.forEach { item ->
                                ElevatedCard(
                                    onClick = { selectedHistory = item },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(item.questionContent.take(30))
                                        }
                                        if (item.isInErrorBook) {
                                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.already_in_error_book)) })
                                        }
                                    }
                                }
                            }
                            if (allHistory.size > recentHistory.size) {
                                OutlinedButton(
                                    onClick = { showAllHistory = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.view_more))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showAllHistory,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            allHistory.forEach { item ->
                                ElevatedCard(
                                    onClick = { selectedHistory = item },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(item.questionContent)
                                        }
                                        if (item.isInErrorBook) {
                                            AssistChip(onClick = {}, label = { Text(stringResource(R.string.already_in_error_book)) })
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { showAllHistory = false },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.collapse))
                            }
                        }
                    }
                }
            } else {
                // Detail Screen: Question text, Image preview, Solve button, Results
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Image Preview
                    if (uiState.imageUri != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp),
                            ) {
                                AsyncImage(
                                    model = uiState.imageUri,
                                    contentDescription = stringResource(R.string.question_image),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.setImageUri(null) }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.remove_image))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.questionText,
                        onValueChange = {
                            viewModel.updateQuestionText(it)
                        },
                        label = { Text(stringResource(R.string.question_supplementary_description_optional)) },
                        shape = RoundedCornerShape(16.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 160.dp),
                        maxLines = 8,
                    )

                    Button(
                        onClick = { viewModel.solveProblem() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = !uiState.isSolving && (uiState.questionText.isNotBlank() || uiState.imageUri != null),
                    ) {
                        if (uiState.isSolving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.parsing))
                        } else {
                            val typeName =
                                when (uiState.selectedTab) {
                                    0 -> stringResource(R.string.geometry)
                                    1 -> stringResource(R.string.algebra)
                                    else -> stringResource(R.string.comprehensive)
                                }
                            Text(stringResource(R.string.start_solving_auto_recognize, typeName))
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.solutionResult.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.problem_solving_result),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                                
                                if (uiState.parsedQuestionContent.isNotBlank()) {
                                    Text("【题目内容】", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafeMarkdownText(markdown = uiState.parsedQuestionContent)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                
                                if (uiState.questionText.isNotBlank()) {
                                    Text("【您的描述】", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(uiState.questionText)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                
                                if (uiState.parsedFinalAnswer.isNotBlank()) {
                                    Text("【最终答案】", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SafeMarkdownText(markdown = uiState.parsedFinalAnswer)
                                } else {
                                    // Fallback if parsing failed
                                    SafeMarkdownText(
                                        markdown = uiState.solutionResult,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showSolutionDetailDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("查看解题详情")
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showErrorBookDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isAddedToErrorBook,
                                ) {
                                    Text(
                                        if (uiState.isAddedToErrorBook) {
                                            stringResource(
                                                R.string.added_to_error_book,
                                            )
                                        } else {
                                            stringResource(R.string.add_to_error_book)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSolutionDetailDialog) {
            Dialog(
                onDismissRequest = { showSolutionDetailDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                )
            ) {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("解题详情") },
                            navigationIcon = {
                                IconButton(onClick = { showSolutionDetailDialog = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                val scope = rememberCoroutineScope()
                                val context = androidx.compose.ui.platform.LocalContext.current
                                IconButton(onClick = {
                                    scope.launch {
                                        val fullContent = buildString {
                                            append("# 解题详情\n\n")
                                            append(uiState.solutionResult)
                                        }
                                        com.example.common.utils.PdfExporter.exportToPdf(
                                            context = context,
                                            title = "解题详情",
                                            content = fullContent,
                                        )
                                    }
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Export PDF")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SafeMarkdownText(
                            markdown = uiState.solutionResult,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (uiState.drawingSteps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val title =
                                when (uiState.selectedTab) {
                                    0 -> stringResource(R.string.geometry_drawing)
                                    1 -> if (uiState.isFunction) stringResource(R.string.function_drawing) else stringResource(R.string.algebra)
                                    else -> when (uiState.comprehensiveType) {
                                        stringResource(R.string.physics) -> stringResource(R.string.physics_diagram)
                                        stringResource(R.string.chemistry) -> stringResource(R.string.chemistry_diagram)
                                        stringResource(R.string.biology) -> stringResource(R.string.biology_diagram)
                                        else -> stringResource(R.string.comprehensive_diagram)
                                    }
                                }
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            val prev = mutableListOf<Map<String, Any>>()
                            uiState.drawingSteps.forEach { step ->
                                GeometryStepCard(
                                    step,
                                    prevShapes = prev.toList(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                )
                                prev += step.shapes
                            }
                        }
                    }
                }
            }
        }

        if (showErrorBookDialog) {
            val geometryStr = stringResource(R.string.geometry)
            val algebraStr = stringResource(R.string.algebra)
            val comprehensiveStr = stringResource(R.string.comprehensive)
            var subject by remember {
                mutableStateOf(
                    when (uiState.selectedTab) {
                        0 -> geometryStr
                        1 -> algebraStr
                        else -> comprehensiveStr
                    },
                )
            }
            var errorReason by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showErrorBookDialog = false },
                title = { Text(stringResource(R.string.add_to_error_book)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text(stringResource(R.string.subject)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = errorReason,
                            onValueChange = { errorReason = it },
                            label = { Text(stringResource(R.string.error_reason_or_knowledge_point)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addToErrorBook(subject, errorReason)
                        showErrorBookDialog = false
                    }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showErrorBookDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        selectedHistory?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedHistory = null },
                title = { Text(stringResource(R.string.problem_solving_details)) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("${item.subject}｜${DateFormatUtils.format(item.timestamp)}")
                        Spacer(modifier = Modifier.height(8.dp))
                        if (item.imageUri != null) {
                            AsyncImage(
                                model = item.imageUri,
                                contentDescription = stringResource(R.string.question_image),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(stringResource(R.string.question_colon), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(item.questionContent)
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.analysis_colon), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        SafeMarkdownText(
                            markdown = item.solution,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedHistory = null }) {
                        Text(stringResource(R.string.close))
                    }
                },
            )
        }
    }
}
