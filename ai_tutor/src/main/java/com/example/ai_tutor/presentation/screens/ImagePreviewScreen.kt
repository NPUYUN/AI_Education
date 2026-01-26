package com.example.ai_tutor.presentation.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ImagePreviewScreen(
    imageUri: String,
    onActionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Bottom Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val actions = listOf("解答一下", "这是什么", "翻译一下")
            
            actions.forEach { action ->
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.White, RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onActionSelected(action) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = action,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
