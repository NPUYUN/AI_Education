// Copyright (c)  2023  Xiaomi Corporation
package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class WaveData(
    val samples: FloatArray,
    val sampleRate: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaveData

        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        return result
    }
}

class WaveReader {
    companion object {
        fun readWave(
            assetManager: AssetManager,
            filename: String,
        ): WaveData? {
            return readWaveFromAsset(assetManager, filename) as? WaveData
        }

        fun readWave(filename: String): WaveData? {
            val obj = readWaveFromFile(filename)
            // The JNI method may return an Object array [samples: FloatArray, sampleRate: Int]
            // instead of a WaveData object depending on the lib version.
            if (obj is Array<*>) {
                try {
                    val samples = obj[0] as FloatArray
                    val sampleRate = obj[1] as Int
                    return WaveData(samples, sampleRate)
                } catch (e: Exception) {
                    return null
                }
            }
            return obj as? WaveData
        }

        // Read a mono wave file asset
        external fun readWaveFromAsset(
            assetManager: AssetManager,
            filename: String,
        ): Any?

        // Read a mono wave file from disk
        external fun readWaveFromFile(filename: String): Any?

        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
