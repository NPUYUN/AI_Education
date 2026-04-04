package com.example.review.planner.models

import com.example.review.planner.presentation.viewmodels.PracticeGradingResult
import com.google.gson.annotations.SerializedName

data class GeneratedProblem(
    @SerializedName("questionText") val questionText: String,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("answer") val answer: String,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("knowledgePointId") val knowledgePointId: String?,
    @SerializedName("difficulty") val difficulty: String?,
    @SerializedName("questionType") val questionType: String?,
    @SerializedName("similarityScore") val similarityScore: Double?,
)

data class GenerateProblemsResponse(
    @SerializedName("problems") val problems: List<GeneratedProblem>,
)

data class GradeTestResponse(
    @SerializedName("results") val results: List<PracticeGradingResult>,
)
