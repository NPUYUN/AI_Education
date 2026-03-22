package com.example.summarizer.video_summarizer.services

import android.content.Context
import android.database.Cursor
import android.net.Uri
import java.io.File
import javax.inject.Inject

class ProcessLocalVideoUseCase @Inject constructor() {
    operator fun invoke(context: Context, uri: Uri): Result<Pair<String, File>> {
        return try {
            val contentResolver = context.contentResolver
            
            // Get file name
            var fileName = "local_video_${System.currentTimeMillis()}.mp4"
            contentResolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { 
                            fileName = it 
                        }
                    }
                }
            }
            
            // Copy to private storage
            val destFile = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Result.success(Pair(fileName, destFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
