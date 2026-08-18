package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.sync.SyncStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = BodhiBhasiApplication::class, sdk = [34])
class SyncTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSyncStatusModel() {
        val idle = SyncStatus.Idle
        assertNotNull(idle)

        val syncing = SyncStatus.Syncing
        assertNotNull(syncing)

        val success = SyncStatus.Success(10, 123456789L)
        assertEquals(10, success.wordsAdded)
        assertEquals(123456789L, success.timestamp)

        val error = SyncStatus.Error("Network error", 987654321L)
        assertEquals("Network error", error.message)
        assertEquals(987654321L, error.timestamp)
    }

    @Test
    fun testAppContainerInitialization() {
        val container = AppContainer(app)
        assertNotNull(container.database)
        assertNotNull(container.repository)
        assertNotNull(container.settingsRepository)
        assertNotNull(container.preferences)
    }
}
