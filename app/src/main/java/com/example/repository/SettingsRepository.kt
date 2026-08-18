package com.example.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lexi_upsc_settings")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.settingsDataStore

    // Enums for settings
    enum class AppTheme { SYSTEM, LIGHT, DARK }
    enum class SyncFrequency { OFF, HOURLY, DAILY, WEEKLY }

    // Keys
    companion object {
        val KEY_THEME = stringPreferencesKey("key_theme")
        val KEY_APPS_SCRIPT_URL = stringPreferencesKey("key_apps_script_url")
        val KEY_GOOGLE_DOC_LINK = stringPreferencesKey("key_google_doc_link")
        val KEY_AUTO_SYNC_FREQUENCY = stringPreferencesKey("key_auto_sync_frequency")
        val KEY_WORD_OF_DAY_ENABLED = booleanPreferencesKey("key_word_of_day_enabled")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("key_dynamic_color")
        val KEY_APP_LANGUAGE = stringPreferencesKey("key_app_language")
        val KEY_DAILY_REVISION_GOAL = intPreferencesKey("key_daily_revision_goal")
        val KEY_REVISION_TIMER_ENABLED = booleanPreferencesKey("key_revision_timer_enabled")
        val KEY_LAST_REVISION_TIMES = stringPreferencesKey("key_last_revision_times")
        
        const val DEFAULT_GOOGLE_DOC_LINK = "https://docs.google.com/spreadsheets/d/1iCL5OMmrn2FtIjXuCmX6hNbrQxxxsM1yk1V48up_L0U/edit?usp=sharing"
        const val DEFAULT_APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxiUX_Bv_hgBo0B07UYRYaycoHs6vQKuavphAHilVC857P3kaD6HAeJvG1Y5hg8AquR/exec"

        fun extractGoogleDocId(input: String): String {
            val trimmed = input.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val docRegex = "/document/d/([a-zA-Z0-9-_]+)".toRegex()
                val sheetRegex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
                docRegex.find(trimmed)?.let { return it.groupValues[1] }
                sheetRegex.find(trimmed)?.let { return it.groupValues[1] }
            }
            return trimmed
        }
    }

    // Streams
    val themeFlow: Flow<AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            val themeStr = prefs[KEY_THEME] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeStr)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }

    val googleDocLinkFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_GOOGLE_DOC_LINK] ?: DEFAULT_GOOGLE_DOC_LINK
        }

    val autoSyncFrequencyFlow: Flow<SyncFrequency> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            val freqStr = prefs[KEY_AUTO_SYNC_FREQUENCY] ?: SyncFrequency.DAILY.name
            try {
                SyncFrequency.valueOf(freqStr)
            } catch (e: Exception) {
                SyncFrequency.DAILY
            }
        }

    val wordOfDayEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_WORD_OF_DAY_ENABLED] ?: true
        }

    val dynamicColorFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_DYNAMIC_COLOR] ?: true
        }

    val appLanguageFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_APP_LANGUAGE] ?: "en"
        }

    val appsScriptUrlFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_APPS_SCRIPT_URL] ?: DEFAULT_APPS_SCRIPT_URL
        }

    val dailyRevisionGoalFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs ->
            prefs[KEY_DAILY_REVISION_GOAL] ?: 10
        }

    val revisionTimerEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs -> prefs[KEY_REVISION_TIMER_ENABLED] ?: false }

    val lastRevisionTimesFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { prefs -> prefs[KEY_LAST_REVISION_TIMES] ?: "" }

    // Setters
    suspend fun setDailyRevisionGoal(goal: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_DAILY_REVISION_GOAL] = goal
        }
    }

    suspend fun setRevisionTimerEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_REVISION_TIMER_ENABLED] = enabled
        }
    }

    suspend fun recordRevisionSessionCompleted(timeOfDayMs: Long) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_LAST_REVISION_TIMES] ?: ""
            val times = current.split(",").filter { it.isNotBlank() }.toMutableList()
            times.add(timeOfDayMs.toString())
            if (times.size > 5) {
                times.removeAt(0)
            }
            prefs[KEY_LAST_REVISION_TIMES] = times.joinToString(",")
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setGoogleDocLink(link: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GOOGLE_DOC_LINK] = link
        }
    }

    suspend fun setAutoSyncFrequency(frequency: SyncFrequency) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_SYNC_FREQUENCY] = frequency.name
        }
    }

    suspend fun setWordOfDayEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_WORD_OF_DAY_ENABLED] = enabled
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language
        }
    }

    suspend fun setAppsScriptUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APPS_SCRIPT_URL] = url
        }
    }
}
