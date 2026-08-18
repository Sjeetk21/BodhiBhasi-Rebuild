package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.state.StatisticsUiState
import com.example.state.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow<UiState<StatisticsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<StatisticsUiState>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.totalWordsCount,
                repository.favoriteWordsCount,
                repository.viewHistory,
                repository.allWords
            ) { totalCount, bookmarksCount, history, words ->
                val distribution = words.groupBy { it.chapter ?: "General" }
                    .mapValues { it.value.size }

                StatisticsUiState(
                    totalWordsCount = totalCount,
                    bookmarkedCount = bookmarksCount,
                    historyCount = history.size,
                    chapterWordDistribution = distribution
                )
            }.catch { e ->
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to compile stats", e)
            }.collect { stats ->
                _uiState.value = UiState.Success(stats)
            }
        }
    }
}
