package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.model.VocabularyWord
import com.example.state.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow<UiState<List<VocabularyWord>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<VocabularyWord>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.viewHistory
                .collect { entities ->
                    if (entities.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        _uiState.value = UiState.Success(entities)
                    }
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearViewHistory()
        }
    }
}
