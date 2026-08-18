package com.example

import android.app.Application
import com.example.database.AppDatabase
import com.example.preferences.UserPreferences
import com.example.repository.WordRepository
import com.example.repository.SettingsRepository
import com.example.sync.SyncManager

/**
 * Dependency container for application-wide instances.
 * This pattern provides compile-time safety, extremely fast build times,
 * and eliminates the runtime and startup overhead of Hilt.
 */
class AppContainer(private val application: Application) {
    
    val database: AppDatabase by lazy {
        android.util.Log.d("LexiUPSC", "DATABASE INIT START")
        val db = AppDatabase.getDatabase(application)
        android.util.Log.d("LexiUPSC", "DATABASE INIT END")
        db
    }
    
    val repository: WordRepository by lazy {
        android.util.Log.d("LexiUPSC", "REPOSITORY CREATED")
        WordRepository(database.wordDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        android.util.Log.d("LexiUPSC", "SETTINGS REPOSITORY CREATED")
        SettingsRepository(application)
    }

    val syncManager: SyncManager by lazy {
        android.util.Log.d("LexiUPSC", "SYNC MANAGER CREATED")
        SyncManager(application, repository, settingsRepository)
    }
    
    val preferences: UserPreferences by lazy {
        android.util.Log.d("LexiUPSC", "PREFERENCES CREATED")
        UserPreferences(application, settingsRepository)
    }
}

class BodhiBhasiApplication : Application() {
    
    lateinit var container: AppContainer

    override fun onCreate() {
        android.util.Log.d("LexiUPSC", "APP STARTED")
        super.onCreate()
        container = AppContainer(this)
    }
}
