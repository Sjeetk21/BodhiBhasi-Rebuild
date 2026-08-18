package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.entity.SyncHistoryEntity
import com.example.sync.SyncStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository
    private val syncManager = app.container.syncManager

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatus.Idle)

    val isNetworkAvailable: StateFlow<Boolean> = syncManager.isNetworkAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val syncHistory: StateFlow<List<SyncHistoryEntity>> = repository.syncHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startManualSync() {
        syncManager.startManualSync()
    }

    fun startBackgroundSync() {
        syncManager.startBackgroundSync()
    }

    fun clearManualSyncStatus() {
        syncManager.clearManualSyncStatus()
    }
}
