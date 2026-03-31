package com.example.ai_tutor.timeline_map.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai_tutor.timeline_map.models.HistoricalEvent
import com.example.ai_tutor.timeline_map.presentation.viewmodels.TimelineMapViewModel
import com.example.common.R
import com.example.common.presentation.components.GlobalApiSettingsDialog
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineMapScreen(
    initialQuery: String? = null,
    viewModel: TimelineMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.updateQuery(initialQuery)
            viewModel.generateTimeline()
        }
    }

    val loading = uiState.isLoading
    val events = uiState.events
    val selectedId = uiState.selectedEventId
    val zoom = uiState.timelineZoom
    val apiKey = uiState.apiKey
    val showSettings = uiState.showApiSettings

    val snackbarHostState = remember { SnackbarHostState() }
    val errorEvent by viewModel.errorEvents.collectAsStateWithLifecycle(initialValue = null)
    val iGotItStr = stringResource(R.string.i_got_it)

    LaunchedEffect(errorEvent) {
        errorEvent?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Long,
                actionLabel = iGotItStr,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initialQuery != null) {
                            stringResource(
                                R.string.initial_query_timeline,
                                initialQuery,
                            )
                        } else {
                            stringResource(R.string.timeline_map)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setApiSettingsVisible(true) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.generating_timeline_map), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (showSettings) {
                GlobalApiSettingsDialog(
                    onDismiss = { viewModel.setApiSettingsVisible(false) },
                )
            }

            AnimatedVisibility(
                visible = !loading && events.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapSection(
                        events = events,
                        selectedId = selectedId,
                        onSelect = { id ->
                            viewModel.selectEvent(id)
                        },
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp)),
                    )

                    // 简略信息卡片（悬浮在地图和时间轴之间）
                    AnimatedVisibility(
                        visible = selectedId != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        val selected = events.find { it.id == selectedId }
                        if (selected != null) {
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selected.time} · ${selected.location}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = selected.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showDetails = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(stringResource(R.string.view_details))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 时间轴（在地图下方）
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.timeline),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                items(events) { e ->
                                    AssistChip(
                                        onClick = { viewModel.selectEvent(e.id) },
                                        label = { Text(e.time) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                            )
                                        },
                                        colors =
                                            AssistChipDefaults.assistChipColors(
                                                containerColor = if (e.id == selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                                labelColor = if (e.id == selectedId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
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
                        title = { Text(stringResource(R.string.event_details)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.time_selected, selected.time), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.location_selected, selected.location),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    stringResource(R.string.people_selected, selected.people.joinToString("、")),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(selected.description, style = MaterialTheme.typography.bodyLarge)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDetails = false }) {
                                Text(stringResource(R.string.back))
                            }
                        },
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
    modifier: Modifier = Modifier,
) {
    val mapView = rememberMapViewWithLifecycle()
    var lastSelectedId by remember { mutableStateOf<String?>(null) }
    var hasInitializedBounds by remember { mutableStateOf(false) }
    var isMapLaidOut by remember { mutableStateOf(false) }
    val contentDesc = stringResource(R.string.historical_event_map_count, events.size)

    Box(modifier = modifier) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription = contentDesc
                    },
            factory = {
                mapView.apply {
                    addOnFirstLayoutListener { v, left, top, right, bottom ->
                        isMapLaidOut = true
                        // 确保初始状态为世界地图
                        controller.setZoom(2.0)
                        controller.setCenter(GeoPoint(0.0, 0.0))
                    }
                }
            },
            update = { view ->
                updateMarkers(view, events, selectedId, onSelect)

                if (selectedId != lastSelectedId && isMapLaidOut) {
                    lastSelectedId = selectedId
                    val selected = events.find { it.id == selectedId }
                    if (selected != null) {
                        view.controller.animateTo(GeoPoint(selected.latitude, selected.longitude))
                    }
                }

                if (!hasInitializedBounds && events.isNotEmpty() && isMapLaidOut) {
                    hasInitializedBounds = true
                    // 根据事件范围调整视野：国家级/世界级
                    val latitudes = events.map { it.latitude }
                    val longitudes = events.map { it.longitude }
                    val minLat = latitudes.minOrNull() ?: 0.0
                    val maxLat = latitudes.maxOrNull() ?: 0.0
                    val minLon = longitudes.minOrNull() ?: 0.0
                    val maxLon = longitudes.maxOrNull() ?: 0.0
                    val latSpan = maxLat - minLat
                    val lonSpan = maxLon - minLon
                    if (latSpan > 50 || lonSpan > 100) {
                        // 跨越大范围，视为世界性
                        view.controller.setZoom(2.5)
                        view.controller.animateTo(GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0))
                    } else {
                        // 使用边界框更贴近国家或区域级别
                        val bbox = BoundingBox(maxLat + 5.0, maxLon + 5.0, minLat - 5.0, minLon - 5.0)
                        view.zoomToBoundingBox(bbox, true)
                    }
                }

                view.invalidate()
            },
        )
    }
}

private fun updateMarkers(
    mapView: MapView,
    events: List<HistoricalEvent>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val context = mapView.context

    mapView.overlays.removeAll { it is Marker }
    events.forEach { e ->
        val marker = Marker(mapView)
        marker.position = GeoPoint(e.latitude, e.longitude)
        marker.title = "${e.time} ${e.location}"
        // 禁用默认的 InfoWindow，改为 Compose UI 处理
        marker.infoWindow = null
        
        val isSelected = e.id == selectedId
        val markerIcon = androidx.core.content.ContextCompat.getDrawable(context, com.example.common.R.drawable.ic_map_marker)?.mutate()
        
        if (markerIcon != null) {
            // 设置选中与非选中状态的颜色
            val tintColor = if (isSelected) {
                android.graphics.Color.parseColor("#E53935") // 选中为红色
            } else {
                android.graphics.Color.parseColor("#1976D2") // 默认未选中为蓝色
            }
            markerIcon.setTint(tintColor)
            
            marker.icon = markerIcon
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        
        marker.setOnMarkerClickListener { _, _ ->
            onSelect(e.id)
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

    val mapView =
        remember {
            MapView(context).apply {
                // 使用国内高德地图作为底图源，提升加载速度和稳定性
                val gaodeTileSource =
                    object :
                        org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                            "GaoDe",
                            1,
                            20,
                            256,
                            ".png",
                            arrayOf(
                                "https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=",
                                "https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=",
                                "https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=",
                                "https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x=",
                            ),
                        ) {
                        override fun getTileURLString(pMapTileIndex: Long): String {
                            return baseUrl + org.osmdroid.util.MapTileIndex.getX(pMapTileIndex) +
                                "&y=" + org.osmdroid.util.MapTileIndex.getY(pMapTileIndex) +
                                "&z=" + org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex)
                        }
                    }
                setTileSource(gaodeTileSource)
                setMultiTouchControls(true)
                setBuiltInZoomControls(true)
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                // 默认加载世界地图视角
                controller.setZoom(2.0)
                controller.setCenter(GeoPoint(0.0, 0.0))
                if (isDark) {
                    overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                }
            }
        }

    DisposableEffect(lifecycleOwner, mapView) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer =
                LifecycleEventObserver { _, event ->
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
