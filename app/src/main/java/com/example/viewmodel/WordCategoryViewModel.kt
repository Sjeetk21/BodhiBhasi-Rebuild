package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.model.VocabularyWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class WordCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow(CategoryState())
    val uiState: StateFlow<CategoryState> = _uiState.asStateFlow()

    data class CategoryState(
        val isLoading: Boolean = false,
        val words: List<VocabularyWord> = emptyList(),
        val selectedCategory: String = "All"
    )

    init {
        loadCategory("All")
    }

    fun loadCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category, isLoading = true)
        
        viewModelScope.launch {
            if (category == "All") {
                repository.allWords
                    .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false) }
                    .collect { words ->
                        _uiState.value = _uiState.value.copy(words = words, isLoading = false)
                    }
            } else {
                repository.getWordsByStatus(category.uppercase())
                    .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false) }
                    .collect { words ->
                        _uiState.value = _uiState.value.copy(words = words, isLoading = false)
                    }
            }
        }
    }
}
