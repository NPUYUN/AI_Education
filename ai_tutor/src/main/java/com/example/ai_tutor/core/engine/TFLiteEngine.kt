package com.example.ai_tutor.core.engine

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.IOException

/**
 * Core AI Engine handling TFLite models with GPU acceleration.
 */
class TFLiteEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    /**
     * Initialize the TFLite interpreter with GPU delegate if available.
     */
    fun initialize(modelPath: String) {
        val options = Interpreter.Options()

        try {
            // Attempt to use GPU Delegate
            val delegateOptions = GpuDelegate.Options()
            gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
        } catch (e: Exception) {
            // Fallback to CPU if GPU delegate fails
            // Log error or just proceed with CPU
            options.setNumThreads(4)
        }

        try {
            val modelBuffer = loadModelFile(modelPath)
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error appropriately
        }
    }

    /**
     * Run inference on input data.
     */
    fun runInference(input: Any, output: Any) {
        interpreter?.run(input, output)
    }

    /**
     * Close and release resources.
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        // This assumes the model is in the assets folder
        // If it's a file path, logic needs adjustment
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
