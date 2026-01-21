package com.example.timeline_map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timeline_map.viewmodel.TimelineViewModel

import androidx.compose.ui.res.stringResource
import com.example.timeline_map.R

@Composable
fun TimelineScreen(viewModel: TimelineViewModel = viewModel()) {
    val speechResult by viewModel.speechResult.collectAsState()
    val events by viewModel.events.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.speech_input_prefix, speechResult))
        
        Button(onClick = { 
            // Mock triggering speech recognition or simulating input
            viewModel.onSpeechResult("辛亥革命关键事件") 
        }) {
            Text(stringResource(R.string.simulate_voice_query_button))
        }

        Text(text = stringResource(R.string.events_found_prefix, events.size))
        events.forEach { event ->
            Text(text = "${event.year}: ${event.title} at (${event.latitude}, ${event.longitude})")
        }
        
        // Placeholder for MapView
        Text(text = stringResource(R.string.map_view_placeholder))
    }
}
