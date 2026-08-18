package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AnalyticsUiState(
    val totalWordsTracked: Int = 0,
    val totalWordsLearned: Int = 0,
    val totalWordsMastered: Int = 0,
    val learningStreak: Int = 0,
    val learningDistribution: Map<String, Int> = emptyMap(),
    val dailyRevisionsHistory: List<Int> = List(30) { 0 },
    // New analytics fields
    val totalBookmarks: Int = 0,
    val wordsAddedThisWeek: Int = 0,
    val wordsAddedThisMonth: Int = 0,
    val averageDailyRevisions: Float = 0f,
    val bestDay: Int = 0,
    val subjectDistribution: List<Pair<String, Int>> = emptyList(),
    val difficultWords: Int = 0,
    val averageDifficulty: Float = 2.5f,
    val averageStability: Float = 1.0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refreshAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                // 1. Fetch Learning Distribution
                val statusCounts = repository.getLearningStatusCounts()
                val distribution = statusCounts.associate { it.status to it.count }

                val totalTracked = distribution.values.sum()
                val mastered = distribution["MASTERED"] ?: 0
                val learned = repository.getWordsLearnedCount()

                // 2. Compute Revision Streak and 30-Day Activity
                val timestamps = repository.getAllRevisionTimestamps()
                var streak = 0
                val historyDays = MutableList(30) { 0 }

                val todayTime = System.currentTimeMillis()
                val calendar = Calendar.getInstance().apply { timeInMillis = todayTime }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis

                if (timestamps.isNotEmpty()) {
                    for (ts in timestamps) {
                        val diffMillis = todayTime - ts
                        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                        if (diffDays in 0..29) {
                            val c = Calendar.getInstance().apply { timeInMillis = ts }
                            c.set(Calendar.HOUR_OF_DAY, 0)
                            c.set(Calendar.MINUTE, 0)
                            c.set(Calendar.SECOND, 0)
                            c.set(Calendar.MILLISECOND, 0)
                            
                            val daysAgo = ((todayStart - c.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                            if (daysAgo in 0..29) {
                                historyDays[daysAgo]++
                            }
                        }
                    }

                    val uniqueDays = timestamps.map { ts ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ts }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }.distinct().sortedDescending()

                    calendar.timeInMillis = todayStart
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStart = calendar.timeInMillis

                    if (uniqueDays.contains(todayStart)) {
                        streak = 1
                        var currentCheck = yesterdayStart
                        for (i in 1 until uniqueDays.size) {
                            if (uniqueDays[i] == currentCheck) {
                                streak++
                                val c = Calendar.getInstance().apply { timeInMillis = currentCheck }
                                c.add(Calendar.DAY_OF_YEAR, -1)
                                currentCheck = c.timeInMillis
                            } else {
                                break
                            }
                        }
                    } else if (uniqueDays.contains(yesterdayStart)) {
                        streak = 1
                        var currentCheck = yesterdayStart
                        val c = Calendar.getInstance().apply { timeInMillis = currentCheck }
                        c.add(Calendar.DAY_OF_YEAR, -1)
                        currentCheck = c.timeInMillis
                        
                        for (i in 1 until uniqueDays.size) {
                            if (uniqueDays[i] == currentCheck) {
                                streak++
                                val c2 = Calendar.getInstance().apply { timeInMillis = currentCheck }
                                c2.add(Calendar.DAY_OF_YEAR, -1)
                                currentCheck = c2.timeInMillis
                            } else {
                                break
                            }
                        }
                    }
                }

                // 3. New analytics data
                val bookmarks = repository.getBookmarkCount()
                
                val weekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }.timeInMillis
                val monthAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }.timeInMillis
                val wordsThisWeek = repository.getWordsAddedSince(weekAgo)
                val wordsThisMonth = repository.getWordsAddedSince(monthAgo)
                
                val activeDays = historyDays.count { it > 0 }
                val avgDaily = if (activeDays > 0) historyDays.sum().toFloat() / 30f else 0f
                val best = historyDays.maxOrNull() ?: 0
                
                val subjectDist = repository.getSubjectDistribution()
                    .map { it.topic to it.count }
                    .take(8)
                
                val difficult = repository.getDifficultWordsCount()
                val avgDiff = repository.getAverageDifficulty().toFloat()
                val avgStab = repository.getAverageStability().toFloat()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalWordsTracked = totalTracked,
                    totalWordsLearned = learned,
                    totalWordsMastered = mastered,
                    learningStreak = streak,
                    learningDistribution = distribution,
                    dailyRevisionsHistory = historyDays,
                    totalBookmarks = bookmarks,
                    wordsAddedThisWeek = wordsThisWeek,
                    wordsAddedThisMonth = wordsThisMonth,
                    averageDailyRevisions = avgDaily,
                    bestDay = best,
                    subjectDistribution = subjectDist,
                    difficultWords = difficult,
                    averageDifficulty = avgDiff,
                    averageStability = avgStab
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }
}
