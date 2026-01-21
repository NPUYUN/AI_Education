package com.example.ai_tutor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.ai_tutor.R
import com.example.ai_tutor.agent.TutorAgent

@Composable
fun ChatScreen() {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<String>() }
    val tutorAgent = remember { TutorAgent() }
    val youPrefix = stringResource(R.string.prefix_you)
    val aiPrefix = stringResource(R.string.prefix_ai)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) }
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            messages.add(String.format(youPrefix, inputText))
                            val response = tutorAgent.processQuery(inputText)
                            messages.add(String.format(aiPrefix, response))
                            inputText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.send_button))
                }
            }
        }
    }
}
