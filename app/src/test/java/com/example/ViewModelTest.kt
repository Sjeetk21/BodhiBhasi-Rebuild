package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.MeaningStyle
import com.example.repository.SettingsRepository
import com.example.state.SearchScope
import com.example.viewmodel.SearchViewModel
import com.example.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = BodhiBhasiApplication::class, sdk = [34])
class ViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsViewModelInitialization() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(app)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testSearchViewModelInitialStateAndMeaningStyle() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(app)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isSearching)
        assertEquals(SearchScope.WORDS, state.searchScope)

        // Verify Meaning Style state flow default and update
        assertEquals(MeaningStyle.SHORT, viewModel.selectedMeaningStyle.value)
        viewModel.setMeaningStyle(MeaningStyle.DESCRIPTIVE)
        assertEquals(MeaningStyle.DESCRIPTIVE, viewModel.selectedMeaningStyle.value)
        viewModel.setMeaningStyle(MeaningStyle.ONE_LINER)
        assertEquals(MeaningStyle.ONE_LINER, viewModel.selectedMeaningStyle.value)
    }
}
