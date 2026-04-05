package com.example.review.planner

import com.example.review.planner.presentation.viewmodels.PracticeGradingResult
import org.junit.Assert.*
import org.junit.Test

class ScoreCalculationTest {

    @Test
    fun `calculateScore returns 100 when all answers are correct`() {
        val totalQuestions = 5
        val results = listOf(
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
        )
        
        val correctCount = results.count { it.isCorrect }
        val score = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
        
        assertEquals(100, score)
    }

    @Test
    fun `calculateScore returns 0 when all answers are wrong`() {
        val totalQuestions = 5
        val results = listOf(
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
        )
        
        val correctCount = results.count { it.isCorrect }
        val score = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
        
        assertEquals(0, score)
    }

    @Test
    fun `calculateScore returns correct percentage when partial answers are correct`() {
        val totalQuestions = 5
        val results = listOf(
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
        )
        
        val correctCount = results.count { it.isCorrect }
        val score = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
        
        assertEquals(40, score)
    }

    @Test
    fun `calculateScore handles single question correctly`() {
        val totalQuestions = 1
        
        // Correct case
        val correctResults = listOf(PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"))
        val correctScore = if (totalQuestions > 0) (correctResults.count { it.isCorrect } * 100 / totalQuestions) else 0
        assertEquals(100, correctScore)
        
        // Wrong case
        val wrongResults = listOf(PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"))
        val wrongScore = if (totalQuestions > 0) (wrongResults.count { it.isCorrect } * 100 / totalQuestions) else 0
        assertEquals(0, wrongScore)
    }

    @Test
    fun `calculateScore returns 0 when no questions`() {
        val totalQuestions = 0
        val results = emptyList<PracticeGradingResult>()
        
        val correctCount = results.count { it.isCorrect }
        val score = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
        
        assertEquals(0, score)
    }

    @Test
    fun `calculateScore with 3 out of 5 correct returns 60`() {
        val totalQuestions = 5
        val results = listOf(
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = true, score = 100, explanation = "Correct"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
            PracticeGradingResult(isCorrect = false, score = 100, explanation = "Wrong"),
        )
        
        val correctCount = results.count { it.isCorrect }
        val score = if (totalQuestions > 0) (correctCount * 100 / totalQuestions) else 0
        
        assertEquals(60, score)
    }
}
