package com.example.ai_tutor.timeline_map.presentation.screens

import com.example.ai_tutor.timeline_map.presentation.viewmodels.TimelineMapViewModel
import com.example.ai_tutor.timeline_map.presentation.viewmodels.TimelineMapUiState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.ai_tutor.timeline_map.models.SpeechLanguage
import com.example.ai_tutor.timeline_map.models.HistoricalEvent
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import org.osmdroid.views.overlay.TilesOverlay
import com.example.common.presentation.components.GlobalApiSettingsDialog
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineMapScreen(
    initialQuery: String? = null,
    viewModel: TimelineMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.updateQuery(initialQuery)
            viewModel.generateTimeline()
        }
    }

    val loading = uiState.isLoading
    val error = uiState.errorMessage
    val events = uiState.events
    val selectedId = uiState.selectedEventId
    val zoom = uiState.timelineZoom
    val apiKey = uiState.apiKey
    val showSettings = uiState.showApiSettings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialQuery != null) "$initialQuery 时间轴" else "时间轴地图") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setApiSettingsVisible(true) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在生成时间轴地图...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (showSettings) {
                GlobalApiSettingsDialog(
                    onDismiss = { viewModel.setApiSettingsVisible(false) }
                )
            }

            AnimatedVisibility(
                visible = !loading && events.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapSection(
                        events = events,
                        selectedId = selectedId,
                        onSelect = { id ->
                            viewModel.selectEvent(id)
                            showDetails = true
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 时间轴（在地图下方）
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "时间轴",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(events) { e ->
                                    AssistChip(
                                        onClick = { viewModel.selectEvent(e.id) },
                                        label = { Text(e.time) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (e.id == selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                            labelColor = if (e.id == selectedId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDetails && selectedId != null) {
                    val selected = events.find { it.id == selectedId }
                    if (selected != null) {
                        AlertDialog(
                            onDismissRequest = { showDetails = false },
                            title = { Text("事件详情") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("时间：${selected.time}", style = MaterialTheme.typography.bodyMedium)
                                    Text("地点：${selected.location}", style = MaterialTheme.typography.bodyMedium)
                                    Text("人物：${selected.people.joinToString("、")}", style = MaterialTheme.typography.bodyMedium)
                                    Text(selected.description, style = MaterialTheme.typography.bodyLarge)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showDetails = false }) {
                                    Text("返回")
                                }
                            }
                        )
                    }
                }
            }
        }
    }



@Composable
private fun MapSection(
    events: List<HistoricalEvent>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapView = rememberMapViewWithLifecycle()
    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            updateMarkers(view, events, onSelect)
            val selected = events.find { it.id == selectedId } ?: events.firstOrNull()
            if (selected != null) {
                view.controller.setCenter(GeoPoint(selected.latitude, selected.longitude))
            }
            // 根据事件范围调整视野：国家级/世界级
            val latitudes = events.map { it.latitude }
            val longitudes = events.map { it.longitude }
            if (latitudes.isNotEmpty() && longitudes.isNotEmpty()) {
                val minLat = latitudes.minOrNull() ?: selected?.latitude ?: 0.0
                val maxLat = latitudes.maxOrNull() ?: selected?.latitude ?: 0.0
                val minLon = longitudes.minOrNull() ?: selected?.longitude ?: 0.0
                val maxLon = longitudes.maxOrNull() ?: selected?.longitude ?: 0.0
                val latSpan = maxLat - minLat
                val lonSpan = maxLon - minLon
                if (latSpan > 50 || lonSpan > 100) {
                    // 跨越大范围，视为世界性
                    view.controller.setZoom(2.5)
                    view.controller.setCenter(GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0))
                } else {
                    // 使用边界框更贴近国家或区域级别
                    val bbox = BoundingBox(maxLat, maxLon, minLat, minLon)
                    view.zoomToBoundingBox(bbox, true)
                }
            }
        }
    )
}

private fun updateMarkers(mapView: MapView, events: List<HistoricalEvent>, onSelect: (String) -> Unit) {
    mapView.overlays.removeAll { it is Marker }
    events.forEach { e ->
        val marker = Marker(mapView)
        marker.position = GeoPoint(e.latitude, e.longitude)
        marker.title = "${e.time} ${e.location}"
        marker.subDescription = e.description
        marker.setOnMarkerClickListener { _, _ ->
            onSelect(e.id)
            marker.showInfoWindow()
            true
        }
        mapView.overlays.add(marker)
    }
    mapView.invalidate()
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val isDark = isSystemInDarkTheme()
    
    val mapView = remember {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        val basePath = File(context.cacheDir, "osmdroid")
        val tilePath = File(basePath, "tiles")
        if (!basePath.exists()) basePath.mkdirs()
        if (!tilePath.exists()) tilePath.mkdirs()
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tilePath
        config.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100MB缓存，提升离线和二次加载速度
        config.tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
        config.load(context, context.getSharedPreferences("osmdroid", 0))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            if (isDark) {
                overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
        }
    }

    return mapView
}






