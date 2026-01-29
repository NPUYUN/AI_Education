package com.example.timeline_map.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.timeline_map.data.model.SpeechLanguage
import com.example.timeline_map.data.model.HistoricalEvent
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TimelineMapScreen(viewModel: TimelineMapViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { }

    val query by viewModel.queryText
    val speechLang by viewModel.speechLanguage
    val listening by viewModel.isListening
    val loading by viewModel.isLoading
    val error by viewModel.errorMessage
    val events = viewModel.events
    val selectedId by viewModel.selectedEventId
    val zoom by viewModel.timelineZoom

    var queryField by remember { mutableStateOf(TextFieldValue(query)) }
    LaunchedEffect(query) {
        if (queryField.text != query) queryField = TextFieldValue(query)
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                viewModel.setListening(false)
            }
            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = texts?.firstOrNull() ?: ""
                viewModel.updateQuery(text)
                viewModel.setListening(false)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = texts?.firstOrNull() ?: ""
                viewModel.updateQuery(text)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening(context, viewModel, speechLang, speechRecognizer) else viewModel.setListening(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时间轴地图") }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.updateSpeechLanguage(SpeechLanguage.ZH) },
                    label = { Text("中文") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (speechLang == SpeechLanguage.ZH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                )
                AssistChip(
                    onClick = { viewModel.updateSpeechLanguage(SpeechLanguage.EN) },
                    label = { Text("English") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (speechLang == SpeechLanguage.EN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                )
                AssistChip(
                    onClick = { viewModel.updateSpeechLanguage(SpeechLanguage.AUTO) },
                    label = { Text("自动") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (speechLang == SpeechLanguage.AUTO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                )
            }
            OutlinedTextField(
                value = queryField,
                onValueChange = {
                    queryField = it
                    viewModel.updateQuery(it.text)
                },
                label = { Text("语音/文本提问") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    enabled = !listening,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        ) {
                            recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            startListening(context, viewModel, speechLang, speechRecognizer)
                        }
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("开始语音")
                }
                Button(enabled = listening, onClick = {
                    speechRecognizer.stopListening()
                    viewModel.setListening(false)
                }) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("停止")
                }
                Button(onClick = { viewModel.generateTimeline() }) {
                    Text("生成时间轴")
                }
            }
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            }
            TimelineBar(
                events = events,
                selectedId = selectedId,
                zoom = zoom,
                onSelect = { viewModel.selectEvent(it) },
                onZoomChange = { viewModel.updateTimelineZoom(it) }
            )
            MapSection(
                events = events,
                selectedId = selectedId,
                onSelect = { viewModel.selectEvent(it) }
            )
            TimelineList(
                events = events,
                selectedId = selectedId,
                zoom = zoom,
                onSelect = { viewModel.selectEvent(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimelineBar(
    events: List<HistoricalEvent>,
    selectedId: String?,
    zoom: Float,
    onSelect: (String) -> Unit,
    onZoomChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Slider(
            value = zoom,
            onValueChange = onZoomChange,
            valueRange = 0.5f..3f
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy((zoom * 8).dp)) {
            items(events) { e ->
                AssistChip(
                    onClick = { onSelect(e.id) },
                    label = { Text("${e.time} • ${e.location}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedId == e.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun MapSection(
    events: List<HistoricalEvent>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    val mapView = rememberMapViewWithLifecycle()
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
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
    val mapView = remember {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        val basePath = File(context.cacheDir, "osmdroid")
        val tilePath = File(basePath, "tiles")
        if (!basePath.exists()) basePath.mkdirs()
        if (!tilePath.exists()) tilePath.mkdirs()
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tilePath
        config.load(context, context.getSharedPreferences("osmdroid", 0))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
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

private fun startListening(
    context: android.content.Context,
    viewModel: TimelineMapViewModel,
    lang: SpeechLanguage,
    recognizer: SpeechRecognizer
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (lang) {
            SpeechLanguage.ZH -> "zh-CN"
            SpeechLanguage.EN -> "en-US"
            SpeechLanguage.AUTO -> ""
        })
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, when (lang) {
            SpeechLanguage.ZH -> "zh-CN"
            SpeechLanguage.EN -> "en-US"
            SpeechLanguage.AUTO -> ""
        })
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
    viewModel.setListening(true)
    recognizer.startListening(intent)
}

@Composable
private fun TimelineList(
    events: List<HistoricalEvent>,
    selectedId: String?,
    zoom: Float,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = events.find { it.id == selectedId }
    Column(modifier = modifier.fillMaxWidth()) {
        if (selected != null) {
            EventDetail(selected, events, onSelect)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy((zoom * 8).dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(events) { e ->
                Card(
                    onClick = { onSelect(e.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedId == e.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = e.time, style = MaterialTheme.typography.titleMedium)
                        Text(text = e.location, style = MaterialTheme.typography.bodyMedium)
                        Text(text = e.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetail(event: HistoricalEvent, events: List<HistoricalEvent>, onSelect: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "事件详情", style = MaterialTheme.typography.titleMedium)
            Text(text = "${event.time} • ${event.location}")
            Text(text = event.description)
            if (event.people.isNotEmpty()) {
                Text(text = "人物：${event.people.joinToString("、")}")
            }
            val related = events.filter { event.relatedIds.contains(it.id) }
            if (related.isNotEmpty()) {
                Text(text = "关联事件")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(related) { r ->
                        AssistChip(
                            onClick = { onSelect(r.id) },
                            label = { Text(r.time) }
                        )
                    }
                }
            }
        }
    }
}
