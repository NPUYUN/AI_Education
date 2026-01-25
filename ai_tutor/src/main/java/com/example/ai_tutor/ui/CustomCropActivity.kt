package com.example.ai_tutor.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import java.io.File
import androidx.core.content.FileProvider
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge

class CustomCropActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Try getting URI from data first, then string extra for backward compatibility
        val uri = intent.data ?: intent.getStringExtra("CROP_URI_STRING")?.let { Uri.parse(it) }
        
        if (uri == null) {
            Toast.makeText(this, "无效的图片URI", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            CustomCropScreen(uri = uri) { resultUri ->
                if (resultUri != null) {
                    val resultIntent = Intent().apply {
                        data = resultUri
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        }
    }
}

@Composable
fun CustomCropScreen(uri: Uri, onResult: (Uri?) -> Unit) {
    var cropImageView by remember { mutableStateOf<CropImageView?>(null) }
    val context = LocalContext.current
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Crop View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // Space for bottom bar
            factory = { ctx ->
                CropImageView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Configuration
                    isShowProgressBar = true
                    isAutoZoomEnabled = true // Auto zoom as requested
                    guidelines = CropImageView.Guidelines.ON
                    scaleType = CropImageView.ScaleType.FIT_CENTER
                    
                    // Set image
                    setImageUriAsync(uri)
                    
                    setOnSetImageUriCompleteListener { view, uri, error ->
                         view.isShowProgressBar = false
                         if (error != null) {
                             Toast.makeText(ctx, "图片加载失败: ${error.message}", Toast.LENGTH_LONG).show()
                         }
                    }
                    
                    setOnCropImageCompleteListener { _, result ->
                        if (result.isSuccessful) {
                            onResult(result.uriContent)
                        } else {
                            Toast.makeText(ctx, "裁剪失败: ${result.error?.message}", Toast.LENGTH_SHORT).show()
                            onResult(null)
                        }
                    }
                    
                    cropImageView = this
                }
            }
        )
        
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            Text(
                text = "取消",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onResult(null) }
                    .padding(8.dp)
            )
            
            // Rotate Button
            Icon(
                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = "Rotate",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { cropImageView?.rotateImage(90) }
            )
        }
        
        // Bottom Bar (Confirm Button)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = { 
                    // Create a file to save the cropped image
                    val cacheDir = File(context.cacheDir, "cropped_images")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val file = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                    val outputUri = Uri.fromFile(file)
                    
                    cropImageView?.croppedImageAsync(customOutputUri = outputUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4) // Google Blue / Standard Action Blue
                )
            ) {
                Text(
                    text = "完成",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
