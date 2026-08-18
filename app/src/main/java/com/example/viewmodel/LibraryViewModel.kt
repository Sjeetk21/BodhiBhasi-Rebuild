package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.model.VocabularyWord
import com.example.state.LibraryUiState
import com.example.state.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow<UiState<LibraryUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<LibraryUiState>> = _uiState.asStateFlow()

    private val _chapterWords = MutableStateFlow<UiState<List<VocabularyWord>>>(UiState.Loading)
    val chapterWords: StateFlow<UiState<List<VocabularyWord>>> = _chapterWords.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allChapters,
                repository.totalWordsCount,
                repository.favoriteWordsCount
            ) { chapters, totalWords, bookmarksCount ->
                LibraryUiState(
                    chapters = chapters,
                    totalWords = totalWords,
                    bookmarkedCount = bookmarksCount
                )
            }.catch { e ->
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to load library statistics", e)
            }.collect { state ->
                if (state.chapters.isEmpty() && state.totalWords == 0) {
                    _uiState.value = UiState.Empty
                } else {
                    _uiState.value = UiState.Success(state)
                }
            }
        }
    }

    fun loadChapterWords(chapterName: String) {
        viewModelScope.launch {
            _chapterWords.value = UiState.Loading
            try {
                repository.allWords.collectLatest { words ->
                    val filtered = words.filter { it.chapter?.equals(chapterName, ignoreCase = true) == true }
                    if (filtered.isEmpty()) {
                        _chapterWords.value = UiState.Empty
                    } else {
                        _chapterWords.value = UiState.Success(filtered)
                    }
                }
            } catch (e: Exception) {
                _chapterWords.value = UiState.Error(e.localizedMessage ?: "Failed to load words for chapter $chapterName", e)
            }
        }
    }
}
