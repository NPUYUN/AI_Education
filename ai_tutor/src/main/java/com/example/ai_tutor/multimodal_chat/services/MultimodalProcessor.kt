package com.example.ai_tutor.multimodal_chat.services

import android.net.Uri

data class MultimodalInput(
    val text: String,
    val imageUri: Uri? = null,
    val audioUri: Uri? = null
)

class MultimodalProcessor {
    // In a real app, this would use SpeechRecognizer for Audio -> Text
    // and a Vision Model for Image -> Text/Embedding
    fun processInput(text: String, image: Uri? = null, audio: Uri? = null): MultimodalInput {
        return MultimodalInput(text, image, audio)
    }
}
