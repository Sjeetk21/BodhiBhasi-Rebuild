package com.example.state

sealed interface UiEvent {
    // Navigation Events
    sealed interface Navigation : UiEvent {
        data class ToWordDetail(val wordId: Int) : Navigation
        data class ToChapter(val chapterName: String) : Navigation
        object ToSearch : Navigation
        object ToLibrary : Navigation
        object ToBookmarks : Navigation
        object ToHistory : Navigation
        object ToStatistics : Navigation
        object ToSettings : Navigation
        object ToSyncProgress : Navigation
        object ToAbout : Navigation
        object Back : Navigation
    }

    // Snackbar Events
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : UiEvent

    // Sync Events
    sealed interface Sync : UiEvent {
        object TriggerManualSync : Sync
        object TriggerBackgroundSync : Sync
        data class SetAutoSyncFrequency(val frequency: com.example.repository.SettingsRepository.SyncFrequency) : Sync
    }

    // Dialog Events
    sealed interface Dialog : UiEvent {
        data class ShowDialog(val dialog: DialogType) : Dialog
        object DismissDialog : Dialog
    }

    // Search Events
    sealed interface Search : UiEvent {
        data class UpdateQuery(val query: String) : Search
        object ClearSearch : Search
    }
}
