package com.example.summarizer.videosummarizer.services

import java.io.File
import javax.inject.Inject

class SummarizeVideoUseCase
    @Inject
    constructor(
        private val summaryRepository: BailianSummaryRepository,
    ) {
        suspend operator fun invoke(
            apiKey: String,
            localFile: File,
            modelName: String,
            baseUrl: String,
            onTranscriptReady: (String) -> Unit,
        ): Result<String> {
            if (!localFile.exists()) {
                return Result.failure(Exception("未找到本地文件，无法上传转写"))
            }

            // 1. Transcribe
            val transcriptResult = summaryRepository.transcribeOffline(localFile)
            val transcript = transcriptResult.getOrNull().orEmpty()
            if (transcript.isBlank()) {
                val message = transcriptResult.exceptionOrNull()?.message ?: "转写失败"
                return Result.failure(Exception(message))
            }

            onTranscriptReady(transcript)

            // 2. Summarize
            val summaryResult = summaryRepository.summarize(apiKey, transcript, modelName, baseUrl)
            val summaryText = summaryResult.getOrNull().orEmpty()
            if (summaryText.isBlank()) {
                val message = summaryResult.exceptionOrNull()?.message ?: "摘要生成失败"
                return Result.failure(Exception(message))
            }

            return Result.success(summaryText)
        }
    }
