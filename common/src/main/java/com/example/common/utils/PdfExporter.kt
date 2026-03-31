package com.example.common.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExporter {
    suspend fun exportToPdf(context: Context, title: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 595 // A4 width at 72 PPI
                val pageHeight = 842 // A4 height at 72 PPI
                val margin = 50f
                
                val titlePaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                
                val contentPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    isAntiAlias = true
                }

                var pageNumber = 1
                var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                // Draw title
                canvas.drawText(title, margin, margin + 24f, titlePaint)
                
                var currentY = margin + 60f
                
                // Content layout
                val textWidth = (pageWidth - 2 * margin).toInt()
                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(content, 0, content.length, contentPaint, textWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(1f, 1.2f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(content, contentPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 1f, false)
                }

                // Draw text line by line to handle pagination
                for (i in 0 until staticLayout.lineCount) {
                    val lineBottom = currentY + staticLayout.getLineBottom(i) - staticLayout.getLineTop(i)
                    if (lineBottom > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = margin
                    }
                    
                    val lineStart = staticLayout.getLineStart(i)
                    val lineEnd = staticLayout.getLineEnd(i)
                    val lineText = content.substring(lineStart, lineEnd)
                    
                    canvas.drawText(lineText, margin, currentY + staticLayout.getLineBaseline(i) - staticLayout.getLineTop(i), contentPaint)
                    currentY += staticLayout.getLineBottom(i) - staticLayout.getLineTop(i)
                }

                pdfDocument.finishPage(page)

                val fileName = "${title}_${System.currentTimeMillis()}.pdf"
                
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/AI_Education")
                    }
                }

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                } else {
                    val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
                    val appDir = java.io.File(dir, "AI_Education").apply { mkdirs() }
                    val file = java.io.File(appDir, fileName)
                    Uri.fromFile(file)
                }

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
                
                pdfDocument.close()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF已保存到文档/AI_Education目录", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出PDF失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
