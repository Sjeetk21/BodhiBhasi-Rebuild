package com.example.engine

import java.util.Calendar

object SpacedRepetitionAlgorithm {

    enum class EvaluationResult {
        GOT_IT,
        ALMOST,
        NEED_REVISION
    }

    data class RevisionData(
        val newLevel: Int,
        val nextRevisionDate: Long,
        val timesRevised: Int,
        val lastRevised: Long
    )

    fun calculateNextRevision(
        currentLevel: Int,
        currentTimesRevised: Int,
        result: EvaluationResult,
        currentTime: Long = System.currentTimeMillis()
    ): RevisionData {
        val newLevel = when (result) {
            EvaluationResult.GOT_IT -> currentLevel + 1
            EvaluationResult.ALMOST -> currentLevel
            EvaluationResult.NEED_REVISION -> Math.max(0, currentLevel - 1)
        }

        val daysToAdd = when (result) {
            EvaluationResult.GOT_IT -> {
                when (newLevel) {
                    1 -> 1
                    2 -> 3
                    3 -> 7
                    4 -> 14
                    5 -> 30
                    else -> 60
                }
            }
            EvaluationResult.ALMOST -> 3
            EvaluationResult.NEED_REVISION -> 1 // Tomorrow
        }

        val nextDate = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, daysToAdd)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return RevisionData(
            newLevel = newLevel,
            nextRevisionDate = nextDate,
            timesRevised = currentTimesRevised + 1,
            lastRevised = currentTime
        )
    }
}
