package com.example.summarizer.text_summarizer.services

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.common.dispatchers.DispatcherProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextExtractionService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dispatcherProvider: DispatcherProvider,
    ) {
        init {
            // Initialize PDFBox
            PDFBoxResourceLoader.init(context)
        }

        suspend fun extractTextFromUri(uri: Uri): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    val contentResolver = context.contentResolver
                    val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromName(uri)

                    val text =
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            when {
                                mimeType.contains("pdf") -> extractFromPdf(inputStream)
                                mimeType.contains("wordprocessingml.document") -> extractFromDocx(inputStream)
                                mimeType.contains("html") -> extractFromHtml(inputStream)
                                mimeType.contains("text/") -> extractFromPlainText(inputStream)
                                else -> throw IllegalArgumentException("不支持的文件格式: $mimeType")
                            }
                        } ?: throw IllegalStateException("无法读取文件内容")

                    if (text.isBlank()) {
                        Result.failure(Exception("文件内容为空或无法提取文本"))
                    } else {
                        Result.success(text.trim())
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        private fun getMimeTypeFromName(uri: Uri): String {
            var name = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            val lowerName = name.lowercase()
            return when {
                lowerName.endsWith(".pdf") -> "application/pdf"
                lowerName.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                lowerName.endsWith(".html") || lowerName.endsWith(".htm") -> "text/html"
                lowerName.endsWith(".txt") || lowerName.endsWith(".csv") -> "text/plain"
                else -> "application/octet-stream"
            }
        }

        private fun extractFromPdf(inputStream: InputStream): String {
            var document: PDDocument? = null
            return try {
                document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                stripper.getText(document)
            } finally {
                document?.close()
            }
        }

        private fun extractFromDocx(inputStream: InputStream): String {
            // A .docx file is a zip archive. The main text is in "word/document.xml"
            val zipIn = ZipInputStream(inputStream)
            var text = ""
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    // Parse XML with Jsoup to extract text content
                    val document = Jsoup.parse(zipIn, "UTF-8", "", org.jsoup.parser.Parser.xmlParser())
                    text = document.text() // Jsoup will extract all text nodes inside the XML tags
                    break
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            return text
        }

        private fun extractFromHtml(inputStream: InputStream): String {
            val document = Jsoup.parse(inputStream, "UTF-8", "")
            return document.text()
        }

        private fun extractFromPlainText(inputStream: InputStream): String {
            return inputStream.bufferedReader().use { it.readText() }
        }
    }
