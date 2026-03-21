package com.example.ai_tutor.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.ai_tutor.data.model.SpeechLanguage
import com.example.ai_tutor.data.model.HistoricalEvent
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import org.osmdroid.views.overlay.TilesOverlay
import com.example.common.presentation.components.GlobalApiSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineMapScreen(
    initialQuery: String? = null,
    viewModel: TimelineMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在生成时间轴地图...", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (showSettings) {
                GlobalApiSettingsDialog(
                    onDismiss = { viewModel.setApiSettingsVisible(false) }
                )
            }

            if (!loading && events.isNotEmpty()) {
                MapSection(
                    events = events,
                    selectedId = selectedId,
                    onSelect = { viewModel.selectEvent(it) },
                    modifier = Modifier.fillMaxSize()
                )
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






