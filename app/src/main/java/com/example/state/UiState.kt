package com.example.state

import com.example.model.VocabularyWord
import com.example.repository.SettingsRepository

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
    object Empty : UiState<Nothing>
}

// Specialised screen UI States
data class HomeUiState(
    val wordOfTheDay: VocabularyWord? = null,
    val chapters: List<String> = emptyList(),
    val recentWords: List<VocabularyWord> = emptyList(),
    val isSyncing: Boolean = false
)

enum class SearchScope { WORDS, SUBJECT, CHAPTER }

data class SearchUiState(
    val query: String = "",
    val searchScope: SearchScope = SearchScope.WORDS,
    val results: List<com.example.model.SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val totalCount: Int = 0
)

data class LibraryUiState(
    val chapters: List<String> = emptyList(),
    val totalWords: Int = 0,
    val bookmarkedCount: Int = 0
)

data class StatisticsUiState(
    val totalWordsCount: Int = 0,
    val bookmarkedCount: Int = 0,
    val historyCount: Int = 0,
    val chapterWordDistribution: Map<String, Int> = emptyMap()
)

data class SettingsUiState(
    val theme: SettingsRepository.AppTheme = SettingsRepository.AppTheme.SYSTEM,
    val googleDocLink: String = "",
    val syncFrequency: SettingsRepository.SyncFrequency = SettingsRepository.SyncFrequency.DAILY,
    val wordOfDayEnabled: Boolean = true,
    val dynamicColorEnabled: Boolean = true,
    val language: String = "en"
)
