package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import com.example.repository.SettingsRepository
import com.example.repository.WordRepository
import com.example.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val wordsAdded: Int, val timestamp: Long) : SyncStatus
    data class Error(val message: String, val timestamp: Long) : SyncStatus
}

class SyncManager(
    private val context: Context,
    private val repository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    private val workManager = WorkManager.getInstance(context)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val externalScope = CoroutineScope(Dispatchers.Default)

    private val _manualSyncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    
    // Combine manual state and background worker status
    val syncStatus: Flow<SyncStatus> = combine(
        _manualSyncStatus,
        workManager.getWorkInfosByTagFlow(SYNC_WORK_TAG)
            .map { workInfos ->
                val activeInfo = workInfos.firstOrNull() ?: return@map SyncStatus.Idle
                when (activeInfo.state) {
                    WorkInfo.State.RUNNING -> SyncStatus.Syncing
                    WorkInfo.State.SUCCEEDED -> {
                        // Success is also recorded in our repository syncHistory, so we can fetch last success count
                        SyncStatus.Success(0, System.currentTimeMillis())
                    }
                    WorkInfo.State.FAILED -> SyncStatus.Error("Background sync task failed", System.currentTimeMillis())
                    else -> SyncStatus.Idle
                }
            }
    ) { manual, background ->
        if (manual is SyncStatus.Syncing || background is SyncStatus.Syncing) {
            SyncStatus.Syncing
        } else if (manual is SyncStatus.Error) {
            manual
        } else if (manual is SyncStatus.Success) {
            manual
        } else {
            background
        }
    }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), SyncStatus.Idle)

    // Flow representing network connectivity status
    val isNetworkAvailable: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(hasInternet)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.stateIn(externalScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Automatically monitor settings changes to re-schedule periodic syncing
        externalScope.launch {
            settingsRepository.autoSyncFrequencyFlow.collect { freq ->
                schedulePeriodicSync(freq)
            }
        }
    }

    /**
     * Executes manual synchronization directly in a coroutine, updating local flow state.
     */
    fun startManualSync() {
        externalScope.launch {
            if (_manualSyncStatus.value is SyncStatus.Syncing) return@launch
            _manualSyncStatus.value = SyncStatus.Syncing
            AppLogger.i("SyncManager: Starting manual sync")

            val isOnline = isNetworkAvailable.first()
            if (!isOnline) {
                _manualSyncStatus.value = SyncStatus.Error("No internet connection available", System.currentTimeMillis())
                return@launch
            }

            try {
                val url = settingsRepository.googleDocLinkFlow.first()
                val result = repository.syncFromGoogleDocs(url)
                if (result.success) {
                    _manualSyncStatus.value = SyncStatus.Success(result.wordsAdded, System.currentTimeMillis())
                } else {
                    _manualSyncStatus.value = SyncStatus.Error(result.errorMessage ?: "Failed to synchronize", System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _manualSyncStatus.value = SyncStatus.Error(e.localizedMessage ?: "Unexpected error", System.currentTimeMillis())
            }
        }
    }

    fun clearManualSyncStatus() {
        _manualSyncStatus.value = SyncStatus.Idle
    }

    /**
     * Enqueues a one-time immediate worker through WorkManager
     */
    fun startBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            MANUAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    /**
     * Sets or modifies periodic background syncing schedules based on SettingsRepository frequency selection.
     */
    fun schedulePeriodicSync(frequency: SettingsRepository.SyncFrequency) {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        
        if (frequency == SettingsRepository.SyncFrequency.OFF) {
            AppLogger.i("SyncManager: Periodic sync is disabled")
            return
        }

        val intervalMinutes = when (frequency) {
            SettingsRepository.SyncFrequency.HOURLY -> 60L
            SettingsRepository.SyncFrequency.DAILY -> 24L * 60L
            SettingsRepository.SyncFrequency.WEEKLY -> 7L * 24L * 60L
            SettingsRepository.SyncFrequency.OFF -> return
        }

        AppLogger.i("SyncManager: Scheduling periodic sync every $intervalMinutes minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    companion object {
        private const val SYNC_WORK_TAG = "lexi_upsc_sync_tag"
        private const val MANUAL_SYNC_WORK_NAME = "lexi_upsc_manual_sync"
        private const val PERIODIC_SYNC_WORK_NAME = "lexi_upsc_periodic_sync"
    }
}
