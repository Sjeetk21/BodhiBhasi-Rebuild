package com.example.engine

import com.example.model.VocabularyWord
import kotlin.math.max

object LexiRevisionEngine {

    enum class Feedback(val modifier: Double) {
        GOT_IT(1.2),
        ALMOST(0.4),
        NEED_REVISION(0.1),
        I_DONT_KNOW(0.1) // Same modifier as Need Revision, but can be handled differently in UI
    }

    /**
     * Calculates the new stability based on feedback.
     * S_new = max(0.1, S_old * Feedback Modifier)
     * 
     * Special case: For "Got It" on a word with difficultyFactor > 1.0,
     * it will slowly decrease difficulty factor but we handle that separately.
     */
    fun calculateNewStability(
        currentStability: Double,
        difficultyFactor: Double,
        feedback: Feedback
    ): Double {
        val multiplier = if (feedback == Feedback.GOT_IT) {
            difficultyFactor // Instead of flat 1.2, use difficulty factor for successful retention
        } else {
            feedback.modifier
        }
        
        return max(0.1, currentStability * multiplier)
    }

    /**
     * Re-evaluates learning status based on stability.
     */
    fun evaluateLearningStatus(
        stability: Double,
        consecutiveFailures: Int,
        currentStatus: String
    ): String {
        if (consecutiveFailures > 2) return "LEARNING"
        return when {
            stability < 2.0 -> "LEARNING"
            stability in 2.0..21.0 -> "FAMILIAR"
            stability > 21.0 -> "MASTERED"
            else -> currentStatus
        }
    }
    
    /**
     * Updates the full metadata record after a review.
     */
    fun processReview(
        word: VocabularyWord,
        feedback: Feedback,
        currentTime: Long = System.currentTimeMillis()
    ): VocabularyWord {
        val consecutiveFailures = if (feedback == Feedback.GOT_IT) 0 else word.consecutiveFailures + 1
        
        // Difficulty factor adjusts based on failures
        val newDifficultyFactor = if (feedback == Feedback.GOT_IT) {
            max(1.2, word.difficultyFactor - 0.1)
        } else {
            word.difficultyFactor + 0.2
        }

        val newStability = calculateNewStability(word.stability, newDifficultyFactor, feedback)
        
        val newStatus = evaluateLearningStatus(newStability, consecutiveFailures, word.learningStatus)
        
        return word.copy(
            stability = newStability,
            lastRevisedTimestamp = currentTime,
            difficultyFactor = newDifficultyFactor,
            consecutiveFailures = consecutiveFailures,
            learningStatus = newStatus
        )
    }
}
