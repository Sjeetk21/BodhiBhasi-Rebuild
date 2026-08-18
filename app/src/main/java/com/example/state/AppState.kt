package com.example.state

import com.example.repository.SettingsRepository
import com.example.sync.SyncStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DialogType {
    object SyncSettings : DialogType
    object ResetConfirm : DialogType
    data class SyncErrorDetails(val error: String) : DialogType
}

sealed interface NavEvent {
    data class NavigateTo(val route: Any, val popUpToRoute: Any? = null, val inclusive: Boolean = false) : NavEvent
    object NavigateBack : NavEvent
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : NavEvent
}

data class AppState(
    val currentRoute: Any? = null,
    val searchQuery: String = "",
    val selectedChapter: String? = null,
    val selectedWordId: Int? = null,
    val theme: SettingsRepository.AppTheme = SettingsRepository.AppTheme.SYSTEM,
    val isLoading: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val snackbarMessage: String? = null,
    val activeDialog: DialogType? = null
)

class AppStateController {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _navEvents = MutableSharedFlow<NavEvent>(extraBufferCapacity = 64)
    val navEvents: SharedFlow<NavEvent> = _navEvents.asSharedFlow()

    fun updateCurrentRoute(route: Any?) {
        _state.value = _state.value.copy(currentRoute = route)
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun selectChapter(chapter: String?) {
        _state.value = _state.value.copy(selectedChapter = chapter)
    }

    fun selectWord(wordId: Int?) {
        _state.value = _state.value.copy(selectedWordId = wordId)
    }

    fun updateTheme(theme: SettingsRepository.AppTheme) {
        _state.value = _state.value.copy(theme = theme)
    }

    fun setLoading(loading: Boolean) {
        _state.value = _state.value.copy(isLoading = loading)
    }

    fun updateSyncStatus(status: SyncStatus) {
        _state.value = _state.value.copy(syncStatus = status)
    }

    fun showSnackbar(message: String?) {
        _state.value = _state.value.copy(snackbarMessage = message)
    }

    fun showDialog(dialog: DialogType?) {
        _state.value = _state.value.copy(activeDialog = dialog)
    }

    fun emitNavEvent(event: NavEvent) {
        _navEvents.tryEmit(event)
    }
}
