package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.state.HomeUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository
    private val syncManager = app.container.syncManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allChapters,
                repository.viewHistory,
                syncManager.syncStatus
            ) { chapters, history, syncStatus ->
                HomeUiState(
                    wordOfTheDay = _uiState.value.wordOfTheDay,
                    chapters = chapters,
                    recentWords = history.take(5),
                    isSyncing = syncStatus is com.example.sync.SyncStatus.Syncing
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }

        loadWordOfTheDay()
    }

    fun loadWordOfTheDay() {
        viewModelScope.launch {
            val randomWord = repository.getRandomWord()
            if (randomWord != null) {
                _uiState.update {
                    it.copy(wordOfTheDay = randomWord)
                }
            }
        }
    }
}
