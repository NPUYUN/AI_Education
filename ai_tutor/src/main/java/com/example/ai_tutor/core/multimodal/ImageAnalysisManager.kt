package com.example.ai_tutor.core.multimodal

import android.content.Context
import android.graphics.Bitmap
import com.example.ai_tutor.core.engine.TFLiteEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AnalysisResult(
    val classification: String,
    val ocrText: String
)

/**
 * Handles image analysis using MobileNet V1 (Quantized) via TFLite and ML Kit OCR.
 */
class ImageAnalysisManager(private val context: Context) {

    private val engine = TFLiteEngine(context)
    private val modelFilename = "mobilenet_v1_1.0_224_quant.tflite"
    private val labelFilename = "labels_mobilenet_quant_v1_224.txt"
    private var labels: List<String> = emptyList()
    
    // OCR
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // MobileNet V1 Quantized constants
    private val inputSize = 224
    private val numClasses = 1001

    fun initialize() {
        try {
            engine.initialize(modelFilename)
            labels = FileUtil.loadLabels(context, labelFilename)
        } catch (e: Exception) {
            e.printStackTrace()
            labels = listOf("Error loading labels")
        }
    }

    fun close() {
        engine.close()
        textRecognizer.close()
    }

    suspend fun analyzeImage(bitmap: Bitmap): AnalysisResult = suspendCoroutine { continuation ->
        if (labels.isEmpty() || labels.first().startsWith("Error")) {
            initialize()
            if (labels.isEmpty()) {
                continuation.resume(AnalysisResult("Error: Model not initialized.", ""))
                return@suspendCoroutine
            }
        }

        // 1. Classification (Synchronous)
        val classificationResult = try {
            runClassification(bitmap)
        } catch (e: Exception) {
            "Classification Error: ${e.message}"
        }

        // 2. OCR (Asynchronous)
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val ocrText = visionText.text
                continuation.resume(AnalysisResult(classificationResult, ocrText))
            }
            .addOnFailureListener { e ->
                continuation.resume(AnalysisResult(classificationResult, "OCR Error: ${e.message}"))
            }
    }

    private fun runClassification(bitmap: Bitmap): String {
        // 1. Preprocess Image
        // Resize to 224x224 and keep as UINT8 for quantized model
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage(DataType.UINT8)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Prepare Output Buffer
        // Output is [1, 1001] UINT8
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, numClasses), DataType.UINT8)

        // 3. Run Inference
        engine.runInference(tensorImage.buffer, outputBuffer.buffer.rewind())

        // 4. Post-process (Get Top 1)
        val labeledProbability = getTopLabel(outputBuffer)
        return "${labeledProbability.first} (${String.format("%.1f%%", labeledProbability.second * 100)})"
    }

    private fun getTopLabel(outputBuffer: TensorBuffer): Pair<String, Float> {
        val bytes = outputBuffer.buffer
        bytes.rewind()
        
        // Find max
        var maxIndex = 0
        var maxVal = 0 // UINT8 is 0-255
        
        val probabilities = FloatArray(numClasses)
        for (i in 0 until numClasses) {
            // Convert byte to unsigned int (0-255)
            val value = bytes.get().toInt() and 0xFF
            probabilities[i] = value / 255.0f
            
            if (value > maxVal) {
                maxVal = value
                maxIndex = i
            }
        }
        
        val label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown"
        return Pair(label, probabilities[maxIndex])
    }
}
