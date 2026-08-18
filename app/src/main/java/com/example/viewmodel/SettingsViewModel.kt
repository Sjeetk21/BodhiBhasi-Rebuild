package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.repository.SettingsRepository
import com.example.state.SettingsUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val settingsRepository = app.container.settingsRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val flow1 = combine(
                settingsRepository.themeFlow,
                settingsRepository.googleDocLinkFlow,
                settingsRepository.autoSyncFrequencyFlow,
                settingsRepository.wordOfDayEnabledFlow
            ) { theme, link, freq, wordOfDay ->
                FourTuple(theme, link, freq, wordOfDay)
            }

            val flow2 = combine(
                settingsRepository.dynamicColorFlow,
                settingsRepository.appLanguageFlow
            ) { dynamicColor, language ->
                dynamicColor to language
            }

            flow1.combine(flow2) { tuple, pair ->
                SettingsUiState(
                    theme = tuple.theme,
                    googleDocLink = tuple.link,
                    syncFrequency = tuple.freq,
                    wordOfDayEnabled = tuple.wordOfDay,
                    dynamicColorEnabled = pair.first,
                    language = pair.second
                )
            }.collect { settings ->
                _uiState.value = settings
            }
        }
    }

    private data class FourTuple<A, B, C, D>(val theme: A, val link: B, val freq: C, val wordOfDay: D)

    fun setTheme(theme: SettingsRepository.AppTheme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setGoogleDocLink(link: String) {
        viewModelScope.launch {
            settingsRepository.setGoogleDocLink(link)
        }
    }

    fun setAutoSyncFrequency(frequency: SettingsRepository.SyncFrequency) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncFrequency(frequency)
        }
    }

    fun setWordOfDayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWordOfDayEnabled(enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setAppLanguage(language)
        }
    }
}
