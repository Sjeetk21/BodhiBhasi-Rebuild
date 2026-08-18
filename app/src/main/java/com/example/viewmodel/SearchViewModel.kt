package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.state.SearchUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val recentSearches: StateFlow<List<String>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _uiState
                .map { Pair(it.query, it.searchScope) }
                .distinctUntilChanged()
                .debounce(150)
                .collectLatest { (query, scope) ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(results = emptyList(), isSearching = false) }
                    } else {
                        _uiState.update { it.copy(isSearching = true) }
                        val results = repository.searchAndRank(query, scope)
                        _uiState.update {
                            it.copy(
                                results = results,
                                isSearching = false,
                                totalCount = results.size
                            )
                        }
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun updateSearchScope(scope: com.example.state.SearchScope) {
        _uiState.update { it.copy(searchScope = scope) }
    }

    fun onSearchExecuted(query: String) {
        viewModelScope.launch {
            if (query.isNotBlank()) {
                repository.addRecentSearch(query)
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", results = emptyList(), totalCount = 0) }
    }

    private val _aiGenerationStatus = MutableStateFlow<String?>(null)
    val aiGenerationStatus: StateFlow<String?> = _aiGenerationStatus.asStateFlow()

    private val _generatedWords = MutableStateFlow<List<com.example.model.VocabularyWord>>(emptyList())
    val generatedWords: StateFlow<List<com.example.model.VocabularyWord>> = _generatedWords.asStateFlow()

    val allChapters: StateFlow<List<String>> = repository.allChapters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSubjects: StateFlow<List<String>> = repository.allSubjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMeaningStyle = MutableStateFlow(com.example.model.MeaningStyle.SHORT)
    val selectedMeaningStyle: StateFlow<com.example.model.MeaningStyle> = _selectedMeaningStyle.asStateFlow()

    fun setMeaningStyle(style: com.example.model.MeaningStyle) {
        _selectedMeaningStyle.value = style
    }

    fun generateWordsWithAi(
        requests: List<com.example.model.AiWordRequest>
    ) {
        viewModelScope.launch {
            _aiGenerationStatus.value = "Generating definitions..."
            _generatedWords.value = emptyList()
            try {
                val settingsRepo = app.container.settingsRepository
                val appsScriptUrl = settingsRepo.appsScriptUrlFlow.first()
                
                val results = repository.generateWordsWithAi(
                    requests = requests,
                    appsScriptUrl = appsScriptUrl,
                    meaningStyle = _selectedMeaningStyle.value
                )
                if (results.isNotEmpty()) {
                    _generatedWords.value = results
                    _aiGenerationStatus.value = "Words generated successfully!"
                } else {
                    _aiGenerationStatus.value = "Failed to parse generated details."
                }
            } catch (e: Exception) {
                _aiGenerationStatus.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun updateGeneratedWordMeaning(index: Int, newMeaning: String) {
        val current = _generatedWords.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(meaning = newMeaning)
            _generatedWords.value = current
        }
    }

    fun saveGeneratedWords(onComplete: () -> Unit) {
        viewModelScope.launch {
            _aiGenerationStatus.value = "Saving words..."
            try {
                val words = _generatedWords.value
                val settingsRepo = app.container.settingsRepository
                val appsScriptUrl = settingsRepo.appsScriptUrlFlow.first()
                repository.saveWordsToDbAndSheet(words, appsScriptUrl)
                _aiGenerationStatus.value = "Words saved successfully!"
                _generatedWords.value = emptyList()
                onComplete()
            } catch (e: Exception) {
                _aiGenerationStatus.value = "Error saving words: ${e.localizedMessage}"
            }
        }
    }

    fun clearAiStatus() {
        _aiGenerationStatus.value = null
        _generatedWords.value = emptyList()
    }
}
