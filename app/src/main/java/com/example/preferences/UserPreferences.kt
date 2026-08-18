package com.example.preferences

import android.content.Context
import com.example.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserPreferences(context: Context, private val settingsRepository: SettingsRepository) {

    private val scope = CoroutineScope(Dispatchers.IO)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeFlow
        .map { theme ->
            when (theme) {
                SettingsRepository.AppTheme.LIGHT -> ThemeMode.LIGHT
                SettingsRepository.AppTheme.DARK -> ThemeMode.DARK
                SettingsRepository.AppTheme.SYSTEM -> ThemeMode.SYSTEM
            }
        }.stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dailyRevisionGoal: StateFlow<Int> = settingsRepository.dailyRevisionGoalFlow
        .stateIn(scope, SharingStarted.Eagerly, 10)

    val isTimerEnabled: StateFlow<Boolean> = settingsRepository.revisionTimerEnabledFlow
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun getThemePreference(): ThemeMode {
        return themeMode.value
    }

    fun setDailyRevisionGoal(goal: Int) {
        scope.launch {
            settingsRepository.setDailyRevisionGoal(goal)
        }
    }

    fun setRevisionTimerEnabled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setRevisionTimerEnabled(enabled)
        }
    }

    fun setThemePreference(mode: ThemeMode) {
        scope.launch {
            val appTheme = when (mode) {
                ThemeMode.LIGHT -> SettingsRepository.AppTheme.LIGHT
                ThemeMode.DARK -> SettingsRepository.AppTheme.DARK
                ThemeMode.SYSTEM -> SettingsRepository.AppTheme.SYSTEM
            }
            settingsRepository.setTheme(appTheme)
        }
    }

    enum class ThemeMode {
        LIGHT,
        DARK,
        SYSTEM
    }
}
