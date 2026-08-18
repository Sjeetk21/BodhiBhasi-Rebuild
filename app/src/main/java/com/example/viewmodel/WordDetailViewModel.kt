package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.model.VocabularyWord
import com.example.state.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WordDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow<UiState<VocabularyWord>>(UiState.Loading)
    val uiState: StateFlow<UiState<VocabularyWord>> = _uiState.asStateFlow()

    fun loadWord(wordId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val vocabularyWord = repository.getWordById(wordId)
                if (vocabularyWord != null) {
                    _uiState.value = UiState.Success(vocabularyWord)
                } else {
                    _uiState.value = UiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to load word details", e)
            }
        }
    }

    fun toggleFavorite(wordId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(wordId)
            val current = _uiState.value
            if (current is UiState.Success) {
                val updatedWord = repository.getWordById(wordId)
                if (updatedWord != null) {
                    _uiState.value = UiState.Success(updatedWord)
                }
            }
        }
    }
}
