package com.example.common.presentation.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.example.common.presentation.components.ChatInputArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size as CoilSize
import android.graphics.drawable.BitmapDrawable

@Composable
fun ImagePreviewScreen(
    imageUri: String,
    onActionSelected: (String, Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Decode URI
    val decodedUriString = remember(imageUri) {
        try {
            java.net.URLDecoder.decode(imageUri, "UTF-8")
        } catch (e: Exception) {
            imageUri
        }
    }

    // State
    var currentUri by remember { mutableStateOf(decodedUriString) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCropping by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // Load Bitmap when URI changes
    LaunchedEffect(currentUri) {
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(currentUri)
                    .size(CoilSize(2048, 2048)) // Limit size to avoid OOM
                    .allowHardware(false) // Essential for Canvas/Bitmap manipulation
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        currentBitmap = drawable.bitmap
                    } else {
                        // Fallback for other drawables
                        val bitmap = Bitmap.createBitmap(
                            drawable.intrinsicWidth,
                            drawable.intrinsicHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        currentBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isCropping && currentBitmap != null) {
            CropOverlay(
                bitmap = currentBitmap!!,
                onConfirm = { croppedBitmap ->
                    scope.launch(Dispatchers.IO) {
                        // Save cropped bitmap to temp file
                        val file = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                        try {
                            val stream = FileOutputStream(file)
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                            stream.flush()
                            stream.close()
                            
                            // Update UI on Main Thread
                            withContext(Dispatchers.Main) {
                                currentUri = Uri.fromFile(file).toString()
                                isCropping = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Crop failed", Toast.LENGTH_SHORT).show()
                                isCropping = false
                            }
                        }
                    }
                },
                onCancel = { isCropping = false }
            )
        } else {
            // Normal Preview Mode
            AsyncImage(
                model = currentUri,
                contentDescription = "Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Top Bar Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close Button (Top Left)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                }

                // Action Buttons (Top Right)
                Row {
                    // Download Button
                    IconButton(
                        onClick = {
                            if (currentBitmap != null && !isDownloading) {
                                isDownloading = true
                                scope.launch(Dispatchers.IO) {
                                    val success = saveImageToGallery(context, currentBitmap!!)
                                    withContext(Dispatchers.Main) {
                                        isDownloading = false
                                        if (success) {
                                            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    
                    // Crop Button
                    IconButton(onClick = { isCropping = true }) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Crop", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // Bottom Layout: Chips + Input Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Suggestion Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val actions = listOf("解答一下", "这是什么", "翻译一下")
                    
                    actions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                                .clickable { onActionSelected(action, Uri.parse(currentUri)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = action,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Chat Input Area
                ChatInputArea(
                    text = inputText,
                    onTextChanged = { inputText = it },
                    onSend = { 
                        if (inputText.isNotBlank()) {
                            onActionSelected(inputText, Uri.parse(currentUri))
                        }
                    },
                    isLoading = false,
                    onVoiceStart = { },
                    onVoiceEnd = { },
                    onCameraClick = { },
                    onGalleryClick = { },
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun CropOverlay(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate image display metrics
    val displayMetrics = remember(containerSize, bitmap) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val containerAspectRatio = containerSize.width.toFloat() / containerSize.height.toFloat()
            
            val displayedWidth: Float
            val displayedHeight: Float
            val offsetX: Float
            val offsetY: Float

            if (imageAspectRatio > containerAspectRatio) {
                // Fit width
                displayedWidth = containerSize.width.toFloat()
                displayedHeight = displayedWidth / imageAspectRatio
                offsetX = 0f
                offsetY = (containerSize.height - displayedHeight) / 2f
            } else {
                // Fit height
                displayedHeight = containerSize.height.toFloat()
                displayedWidth = displayedHeight * imageAspectRatio
                offsetY = 0f
                offsetX = (containerSize.width - displayedWidth) / 2f
            }
            DisplayMetrics(displayedWidth, displayedHeight, offsetX, offsetY)
        } else {
            null
        }
    }

    // Initialize cropRect to full image when metrics are ready
    LaunchedEffect(displayMetrics) {
        if (displayMetrics != null && cropRect == null) {
            cropRect = Rect(
                displayMetrics.offsetX,
                displayMetrics.offsetY,
                displayMetrics.offsetX + displayMetrics.width,
                displayMetrics.offsetY + displayMetrics.height
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消", color = Color.White, fontSize = 18.sp)
            }
            // Optional: Add Reset or Rotate buttons here if needed
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                }
        ) {
            if (displayMetrics != null) {
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var startPoint = Offset.Zero
                            detectDragGestures(
                                onDragStart = { offset ->
                                    startPoint = offset
                                    // Start new selection
                                    cropRect = Rect(offset, offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentPoint = change.position
                                    val x1 = startPoint.x.coerceIn(displayMetrics.offsetX, displayMetrics.offsetX + displayMetrics.width)
                                    val y1 = startPoint.y.coerceIn(displayMetrics.offsetY, displayMetrics.offsetY + displayMetrics.height)
                                    val x2 = currentPoint.x.coerceIn(displayMetrics.offsetX, displayMetrics.offsetX + displayMetrics.width)
                                    val y2 = currentPoint.y.coerceIn(displayMetrics.offsetY, displayMetrics.offsetY + displayMetrics.height)
                                    
                                    val left = min(x1, x2)
                                    val top = min(y1, y2)
                                    val right = max(x1, x2)
                                    val bottom = max(y1, y2)
                                    
                                    cropRect = Rect(left, top, right, bottom)
                                }
                            )
                        }
                ) {
                    // Draw Image
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(displayMetrics.offsetX.toInt(), displayMetrics.offsetY.toInt()),
                        dstSize = IntSize(displayMetrics.width.toInt(), displayMetrics.height.toInt())
                    )
                    
                    // Draw Overlay
                    cropRect?.let { rect ->
                        // Dimming Outside
                        // Top
                        drawRect(Color.Black.copy(alpha = 0.6f), Offset.Zero, Size(size.width, rect.top))
                        // Bottom
                        drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, rect.bottom), Size(size.width, size.height - rect.bottom))
                        // Left
                        drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, rect.top), Size(rect.left, rect.height))
                        // Right
                        drawRect(Color.Black.copy(alpha = 0.6f), Offset(rect.right, rect.top), Size(size.width - rect.right, rect.height))
                        
                        // Border
                        drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(2.dp.toPx()))
                        
                        // Grid Lines (Rule of Thirds)
                        val thirdW = rect.width / 3
                        val thirdH = rect.height / 3
                        val gridColor = Color.White.copy(alpha = 0.5f)
                        val gridStroke = 1.dp.toPx()
                        
                        drawLine(gridColor, Offset(rect.left + thirdW, rect.top), Offset(rect.left + thirdW, rect.bottom), gridStroke)
                        drawLine(gridColor, Offset(rect.left + thirdW * 2, rect.top), Offset(rect.left + thirdW * 2, rect.bottom), gridStroke)
                        drawLine(gridColor, Offset(rect.left, rect.top + thirdH), Offset(rect.right, rect.top + thirdH), gridStroke)
                        drawLine(gridColor, Offset(rect.left, rect.top + thirdH * 2), Offset(rect.right, rect.top + thirdH * 2), gridStroke)
                        
                        // Corner Handles (Thick L-shapes)
                        val cornerLen = 30.dp.toPx()
                        val cornerStroke = 6.dp.toPx()
                        val cornerColor = Color.White
                        
                        // Top Left
                        drawLine(cornerColor, rect.topLeft, Offset(rect.left + cornerLen, rect.top), cornerStroke)
                        drawLine(cornerColor, rect.topLeft, Offset(rect.left, rect.top + cornerLen), cornerStroke)
                        
                        // Top Right
                        drawLine(cornerColor, rect.topRight, Offset(rect.right - cornerLen, rect.top), cornerStroke)
                        drawLine(cornerColor, rect.topRight, Offset(rect.right, rect.top + cornerLen), cornerStroke)
                        
                        // Bottom Left
                        drawLine(cornerColor, rect.bottomLeft, Offset(rect.left + cornerLen, rect.bottom), cornerStroke)
                        drawLine(cornerColor, rect.bottomLeft, Offset(rect.left, rect.bottom - cornerLen), cornerStroke)
                        
                        // Bottom Right
                        drawLine(cornerColor, rect.bottomRight, Offset(rect.right - cornerLen, rect.bottom), cornerStroke)
                        drawLine(cornerColor, rect.bottomRight, Offset(rect.right, rect.bottom - cornerLen), cornerStroke)
                    }
                }
            }
        }

        // Bottom Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (displayMetrics != null && cropRect != null) {
                        val rect = cropRect!!
                        if (rect.width > 0 && rect.height > 0) {
                            val scaleX = bitmap.width / displayMetrics.width
                            val scaleY = bitmap.height / displayMetrics.height
                            
                            val cropLeft = ((rect.left - displayMetrics.offsetX) * scaleX).toInt().coerceIn(0, bitmap.width)
                            val cropTop = ((rect.top - displayMetrics.offsetY) * scaleY).toInt().coerceIn(0, bitmap.height)
                            val cropWidth = (rect.width * scaleX).toInt().coerceIn(1, bitmap.width - cropLeft)
                            val cropHeight = (rect.height * scaleY).toInt().coerceIn(1, bitmap.height - cropTop)
                            
                            try {
                                val cropped = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
                                onConfirm(cropped)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)) // Light Blue
            ) {
                Text("完成", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

private data class DisplayMetrics(
    val width: Float,
    val height: Float,
    val offsetX: Float,
    val offsetY: Float
)

fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    uri?.let {
        try {
            resolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    return false
}
