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

class RevisionDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository
    private val preferences = app.container.preferences

    val isTimerEnabled: StateFlow<Boolean> = preferences.isTimerEnabled

    data class DashboardState(
        val isLoading: Boolean = true,
        val wordsDueToday: Int = 0,
        val wordsRevisedToday: Int = 0,
        val revisionStreak: Int = 0,
        val wordsLearned: Int = 0,
        val estimatedSessionTime: Int = 5, // minutes
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    fun refreshDashboard() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val todayTime = System.currentTimeMillis()
                
                // Calculate today's boundaries
                val calendar = Calendar.getInstance().apply { timeInMillis = todayTime }
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis

                val revisedToday = repository.getWordsRevisedTodayCount(startOfDay, endOfDay)
                
                val targetSize = preferences.dailyRevisionGoal.value
                val remainingTarget = Math.max(0, targetSize - revisedToday)
                val sessionWords = repository.generateRevisionSession(todayTime, remainingTarget, startOfDay)
                
                val learned = repository.getWordsLearnedCount()
                
                val timestamps = repository.getAllRevisionTimestamps()
                var streak = 0
                
                if (timestamps.isNotEmpty()) {
                    val uniqueDays = timestamps.map { ts ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ts }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }.distinct().sortedDescending()

                    val todayCal = Calendar.getInstance().apply { timeInMillis = todayTime }
                    todayCal.set(Calendar.HOUR_OF_DAY, 0)
                    todayCal.set(Calendar.MINUTE, 0)
                    todayCal.set(Calendar.SECOND, 0)
                    todayCal.set(Calendar.MILLISECOND, 0)
                    val todayStart = todayCal.timeInMillis
                    
                    todayCal.add(Calendar.DAY_OF_YEAR, -1)
                    val trueYesterdayStart = todayCal.timeInMillis

                    if (uniqueDays.contains(todayStart)) {
                        streak = 1
                        var currentCheck = trueYesterdayStart
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
                    } else if (uniqueDays.contains(trueYesterdayStart)) {
                        streak = 1
                        var currentCheck = trueYesterdayStart
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
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    wordsDueToday = sessionWords.size,
                    wordsRevisedToday = revisedToday,
                    revisionStreak = streak,
                    wordsLearned = learned,
                    estimatedSessionTime = Math.max(5, (sessionWords.size * 15) / 60)
                )
            } catch (e: Exception) {
                android.util.Log.e("RevisionDashboard", "Error refreshing dashboard", e)
                val stackTrace = android.util.Log.getStackTraceString(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    wordsDueToday = 0,
                    errorMessage = "${e.message}\n$stackTrace"
                )
            }
        }
    }
}
