package com.example.ai_tutor.ui

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CustomCropScreen(
    imageUri: Uri,
    onCropSuccess: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cropImageView = remember { CropImageView(context) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                cropImageView.apply {
                    setImageUriAsync(imageUri)
                    isShowCropOverlay = true
                    isAutoZoomEnabled = true
                    setOnCropImageCompleteListener { _, result ->
                         if (result.isSuccessful) {
                             val bitmap = result.bitmap
                             if (bitmap != null) {
                                 onCropSuccess(bitmap)
                             }
                         }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top-Left Checkmark Button
        IconButton(
            onClick = {
                // Perform crop on background thread
                scope.launch(Dispatchers.Default) {
                    cropImageView.croppedImageAsync()
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Crop & Send",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
