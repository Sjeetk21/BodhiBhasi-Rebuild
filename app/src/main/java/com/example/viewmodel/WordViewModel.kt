package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BodhiBhasiApplication
import com.example.entity.SyncHistoryEntity
import com.example.model.VocabularyWord
import com.example.model.SyncPreviewData
import com.example.model.SyncDebugReport
import com.example.model.Step1Report
import com.example.model.Step2Report
import com.example.model.Step3Report
import com.example.model.Step4Report
import com.example.model.Step5Report
import android.content.Context
import com.example.parser.SpreadsheetParser
import com.example.preferences.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

class WordViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BodhiBhasiApplication
    private val repository = app.container.repository
    private val preferences = app.container.preferences
    private val settingsRepository = app.container.settingsRepository

    val googleDocLink: StateFlow<String> = settingsRepository.googleDocLinkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val appsScriptUrl: StateFlow<String> = settingsRepository.appsScriptUrlFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Theme Mode
    val themeMode: StateFlow<UserPreferences.ThemeMode> = preferences.themeMode
    
    // Settings
    val dailyRevisionGoal: StateFlow<Int> = preferences.dailyRevisionGoal
    val isTimerEnabled: StateFlow<Boolean> = preferences.isTimerEnabled

    // Core words & UI states
    val allWords: StateFlow<List<VocabularyWord>> = repository.allWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteWords: StateFlow<List<VocabularyWord>> = repository.favoriteWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<String>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val viewHistory: StateFlow<List<VocabularyWord>> = repository.viewHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChapters: StateFlow<List<String>> = repository.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWordsCount: StateFlow<Int> = repository.totalWordsCount
        .onStart { android.util.Log.d("LexiUPSC", "PLAYER FLOW CREATED") }
        .onEach { android.util.Log.d("LexiUPSC", "PLAYER FLOW EMITTED: count=$it") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val favoriteWordsCount: StateFlow<Int> = repository.favoriteWordsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncHistory: StateFlow<List<SyncHistoryEntity>> = repository.syncHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Words added today Flow
    private val _wordsAddedTodayCount = MutableStateFlow(0)
    val wordsAddedTodayCount: StateFlow<Int> = _wordsAddedTodayCount.asStateFlow()

    // Interactive States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.example.model.SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<com.example.model.SearchResultItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _randomWord = MutableStateFlow<VocabularyWord?>(null)
    val randomWord: StateFlow<VocabularyWord?> = _randomWord.asStateFlow()

    private val _isRandomWordLoading = MutableStateFlow(false)
    val isRandomWordLoading: StateFlow<Boolean> = _isRandomWordLoading.asStateFlow()

    private val _isRandomWordVisible = MutableStateFlow(false)
    val isRandomWordVisible: StateFlow<Boolean> = _isRandomWordVisible.asStateFlow()

    // Track recently shown random word IDs to avoid repetition (keep last 20)
    private val recentRandomIds = mutableListOf<Int>()
    private val maxRecentRandomHistory = 20

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    private val _syncPreview = MutableStateFlow<SyncPreviewData?>(null)
    val syncPreview: StateFlow<SyncPreviewData?> = _syncPreview.asStateFlow()

    private val _syncDebugReport = MutableStateFlow<SyncDebugReport?>(null)
    val syncDebugReport: StateFlow<SyncDebugReport?> = _syncDebugReport.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        android.util.Log.d("LexiUPSC", "VIEWMODEL CREATED")
        updateWordsAddedToday()
        // Automatic setup: check if database is empty first and load demo vocabulary only if count is 0
        viewModelScope.launch {
            if (repository.getWordCount() == 0) {
                loadDemoUpscVocabulary()
            }
        }

        // Search engine listener
        viewModelScope.launch {
            searchQuery
                .debounce(150) // Premium responsiveness: small debounce for ultra-snappy feedback
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        val ranked = repository.searchAndRank(query)
                        _searchResults.value = ranked
                        _isSearching.value = false
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: UserPreferences.ThemeMode) {
        preferences.setThemePreference(mode)
    }

    fun setDailyRevisionGoal(goal: Int) {
        preferences.setDailyRevisionGoal(goal)
    }

    fun setRevisionTimerEnabled(enabled: Boolean) {
        preferences.setRevisionTimerEnabled(enabled)
    }

    fun onSearchExecuted(query: String) {
        viewModelScope.launch {
            repository.addRecentSearch(query)
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    fun clearViewHistory() {
        viewModelScope.launch {
            repository.clearViewHistory()
        }
    }

    fun toggleFavorite(wordId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(wordId)
        }
    }

    fun viewWord(wordId: Int) {
        viewModelScope.launch {
            repository.recordWordView(wordId)
            repository.getWordById(wordId)
        }
    }

    fun loadRandomWord() {
        viewModelScope.launch {
            _isRandomWordVisible.value = true
            _isRandomWordLoading.value = true
            try {
                val word = repository.getWeightedRandomWord(recentRandomIds)
                if (word != null) {
                    // Track this word ID in recent history
                    recentRandomIds.add(word.id)
                    // Keep history bounded
                    if (recentRandomIds.size > maxRecentRandomHistory) {
                        recentRandomIds.removeAt(0)
                    }
                    _randomWord.value = word
                } else {
                    // Fallback: if weighted random returns null, try plain random
                    val fallback = repository.getRandomWord()
                    _randomWord.value = fallback
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("WordViewModel", "Failed to load random word", e)
                _randomWord.value = null
            } finally {
                _isRandomWordLoading.value = false
            }
        }
    }

    fun dismissRandomWord() {
        _isRandomWordVisible.value = false
        _randomWord.value = null
    }

    private fun updateWordsAddedToday() {
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            repository.getWordsAddedTodayCount(calendar.timeInMillis).collectLatest { count ->
                _wordsAddedTodayCount.value = count
            }
        }
    }

    /**
     * Triggers phase 1: download and parse from primary Google Doc source for preview.
     */
    fun previewGoogleDocSync(url: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            _importStatus.value = "Downloading..."
            _syncPreview.value = null
            _syncDebugReport.value = null
            try {
                val syncUrl = url ?: settingsRepository.googleDocLinkFlow.first()
                val preview = repository.previewGoogleDocSync(syncUrl)
                _syncPreview.value = preview
                if (!preview.step4.success) {
                    val errMsg = preview.step1.errorMessage ?: preview.step3.errorMessage ?: preview.step4.parserErrors.firstOrNull() ?: "Parser or download failed."
                    _importStatus.value = "Sync preview failed: $errMsg"
                } else {
                    _importStatus.value = "Preview ready."
                }
            } catch (e: Exception) {
                _importStatus.value = "Failed to fetch preview: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Triggers phase 2: database commit from the user-approved preview.
     */
    fun confirmImportAndSync(preview: SyncPreviewData) {
        viewModelScope.launch {
            _isSyncing.value = true
            _importStatus.value = "Saving to database..."
            try {
                val startTime = System.currentTimeMillis()
                val report = repository.importParsedGoogleDocWords(
                    parsedWords = preview.parsedWords,
                    step1 = preview.step1,
                    step2 = preview.step2,
                    step3 = preview.step3,
                    step4 = preview.step4,
                    startTimeMs = startTime
                )
                _syncDebugReport.value = report
                _syncPreview.value = null // Close the preview dialog
                _importStatus.value = "Import complete. ${report.step5.imported} imported, ${report.step5.updated} updated."
            } catch (e: Exception) {
                _importStatus.value = "Database import failed: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncStates() {
        _syncPreview.value = null
        _syncDebugReport.value = null
    }

    /**
     * Performs synchronized download and parsing from primary Google Doc source (retained for backward compatibility / background workers).
     */
    fun syncFromGoogleDocs(url: String? = null) {
        viewModelScope.launch {
            _importStatus.value = "Importing..."
            try {
                val syncUrl = url ?: settingsRepository.googleDocLinkFlow.first()
                val result = repository.syncFromGoogleDocs(syncUrl)
                if (result.success) {
                    _importStatus.value = """
                        Completed.
                        Imported: ${result.imported}
                        Updated: ${result.updated}
                        Skipped: ${result.skipped}
                        Failed: ${result.failed}
                    """.trimIndent()
                } else {
                    _importStatus.value = "Synchronization failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _importStatus.value = "Synchronization failed: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Parses a local DOCX file selected by the user, inserting results into the DB.
     */
    fun importSpreadsheetFile(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = "Importing..."
            try {
                val context = getApplication<Application>()
                val inputStream: java.io.InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    _importStatus.value = "Parsing..."
                    val fileName = getFileName(uri, context)
                    val words = if (fileName.endsWith(".xlsx", ignoreCase = true)) {
                        SpreadsheetParser.parseXlsxToWords(inputStream)
                    } else {
                        SpreadsheetParser.parseCsvStreamToWords(inputStream)
                    }
                    if (words.isNotEmpty()) {
                        _importStatus.value = "Saving..."
                        repository.insertWords(words)
                        _importStatus.value = "Completed."
                    } else {
                        _importStatus.value = "No valid vocabulary structures found in the file."
                    }
                } else {
                    _importStatus.value = "Failed to open document file."
                }
            } catch (e: Exception) {
                _importStatus.value = "Error parsing file: ${e.localizedMessage}"
            }
        }
    }

    private fun getFileName(uri: Uri, context: Context): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        result = cursor.getString(idx)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: ""
    }

    fun setGoogleDocLink(link: String) {
        viewModelScope.launch {
            settingsRepository.setGoogleDocLink(link)
        }
    }

    fun setAppsScriptUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setAppsScriptUrl(url)
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    /**
     * Export database content as formatted Text/JSON that can be shared.
     */
    fun getDatabaseExportString(): String {
        val words = allWords.value
        val sb = StringBuilder()
        sb.append("LexiUPSC Dictionary Backup\n")
        sb.append("Generated on: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n")
        sb.append("Total words: ${words.size}\n\n")
        
        words.forEach { word ->
            sb.append("Word: ${word.word}\n")
            if (!word.pronunciation.isNullOrEmpty()) sb.append("Pronunciation: ${word.pronunciation}\n")
            sb.append("Meaning: ${word.meaning}\n")
            word.examples.forEach { sb.append("Example: $it\n") }
            if (!word.baseForm.isNullOrEmpty()) sb.append("Base Form: ${word.baseForm}\n")
            if (!word.otherForms.isNullOrEmpty()) sb.append("Other Forms: ${word.otherForms}\n")
            if (!word.relatedForms.isNullOrEmpty()) sb.append("Related Forms: ${word.relatedForms}\n")
            if (!word.memoryHook.isNullOrEmpty()) sb.append("Memory Hook: ${word.memoryHook}\n")
            if (!word.topic.isNullOrEmpty()) sb.append("Topic: ${word.topic}\n")
            if (!word.chapter.isNullOrEmpty()) sb.append("Chapter: ${word.chapter}\n")
            sb.append("\n---\n\n")
        }
        return sb.toString()
    }

    /**
     * Seeds the empty Room Database with dynamic premium UPSC word lists covering key subjects.
     */
    private suspend fun loadDemoUpscVocabulary() {
        val demoList = listOf(
            VocabularyWord(
                word = "Hegemony",
                pronunciation = "/hɪˈdʒɛməni/",
                meaning = "Dominance, influence, or authority, especially of one nation, state, or social group over others.",
                examples = listOf(
                    "The post-Cold War era witnessed the economic hegemony of Western institutions.",
                    "UPSC aspirants must understand how cultural hegemony shapes public administration policies."
                ),
                baseForm = "Hegemon",
                otherForms = "Hegemonies, Hegemonic",
                relatedForms = "Hegemonist",
                memoryHook = "Hegemony sounds like 'Huge Money' - countries with huge money usually establish dominance over others.",
                topic = "International Relations",
                chapter = "Polity & IR"
            ),
            VocabularyWord(
                word = "Arbitrage",
                pronunciation = "/ˈɑːrbɪtrɑːʒ/",
                meaning = "The simultaneous buying and selling of securities, currency, or commodities in different markets to take advantage of differing prices.",
                examples = listOf(
                    "Regulatory arbitrage allows multinational firms to exploit differences in tax jurisdictions."
                ),
                baseForm = "Arbitrage",
                otherForms = "Arbitraging, Arbitraged",
                relatedForms = "Arbitrageur",
                memoryHook = "Arbitrage = 'Arbitrary Advantage' - taking advantage of pricing gaps across different places.",
                topic = "Financial Markets",
                chapter = "Economy"
            ),
            VocabularyWord(
                word = "Sovereignty",
                pronunciation = "/ˈsɒvrənti/",
                meaning = "Supreme power or authority; the authority of a state to govern itself or another state.",
                examples = listOf(
                    "The Preamble to the Indian Constitution declares India to be a Sovereign country."
                ),
                baseForm = "Sovereign",
                otherForms = "Sovereignties",
                relatedForms = "Sovereignist",
                memoryHook = "Sovereign = 'Reign' over a single 'Zone'. It means absolute supreme ruling power.",
                topic = "Constitutional Law",
                chapter = "Polity & IR"
            ),
            VocabularyWord(
                word = "Fraternization",
                pronunciation = "/ˌfrætənaɪˈzeɪʃən/",
                meaning = "Associate or form friendship with someone, especially when one is not supposed to.",
                examples = listOf(
                    "Civil service conduct rules forbid improper fraternization with private contractors during active bids."
                ),
                baseForm = "Fraternize",
                otherForms = "Fraternized, Fraternizing",
                relatedForms = "Fraternity, Fraternal",
                memoryHook = "Frater = Brother in Latin. Fraternization means treating someone like a brother or associate.",
                topic = "Ethics in Administration",
                chapter = "Ethics & Integrity"
            ),
            VocabularyWord(
                word = "Gerrymandering",
                pronunciation = "/ˌdʒɛriˈmændərɪŋ/",
                meaning = "Manipulate the boundaries of an electoral constituency so as to favor one party or class.",
                examples = listOf(
                    "The Delimitation Commission ensures fair representation and prevents any form of political gerrymandering in India."
                ),
                baseForm = "Gerrymander",
                otherForms = "Gerrymandered, Gerrymanders",
                relatedForms = "Gerrymanderer",
                memoryHook = "Gerry (Governor Elbridge Gerry) + Salamander (the bizarre shape of the manipulated district looks like a lizard!).",
                topic = "Electoral Processes",
                chapter = "Polity & IR"
            ),
            VocabularyWord(
                word = "Pluralism",
                pronunciation = "/ˈplʊərəˌlɪzəm/",
                meaning = "A system in which two or more states, groups, principles, or sources of authority coexist.",
                examples = listOf(
                    "India's rich social pluralism is protected under fundamental cultural and educational rights."
                ),
                baseForm = "Plural",
                otherForms = "Pluralist, Pluralistic",
                relatedForms = "Plurality",
                memoryHook = "Plural = More than one. Pluralism means a society built on multiple coexisting groups.",
                topic = "Indian Society",
                chapter = "Social Issues"
            ),
            VocabularyWord(
                word = "Precipitation",
                pronunciation = "/prɪˌsɪpɪˈteɪʃən/",
                meaning = "Any product of the condensation of atmospheric water vapor that falls under gravitational pull (rain, snow, sleet).",
                examples = listOf(
                    "Orographic precipitation plays a major role in the climate profile of the Western Ghats."
                ),
                baseForm = "Precipitate",
                otherForms = "Precipitations",
                relatedForms = "Precipitous",
                memoryHook = "Precipitate = falling down rapidly. Precipitation refers to rain falling from the clouds.",
                topic = "Climatology",
                chapter = "Geography"
            ),
            VocabularyWord(
                word = "Interregnum",
                pronunciation = "/ˌɪntəˈrɛɡnəm/",
                meaning = "A period when normal government is suspended, especially between successive reigns or regimes.",
                examples = listOf(
                    "The peaceful interregnum between the declaration of election results and the official oath ceremony."
                ),
                baseForm = "Regnum",
                otherForms = "Interregnums",
                relatedForms = "Interregnal",
                memoryHook = "Inter (Between) + Regnum (Reign/Kingdom) = Period between two consecutive rules/reigns.",
                topic = "Constitutional History",
                chapter = "Modern History"
            ),
            VocabularyWord(
                word = "Epistemology",
                pronunciation = "/ɪˌpɪstɪˈmɒlədʒi/",
                meaning = "The theory of knowledge, especially with regard to its methods, validity, and scope.",
                examples = listOf(
                    "A critical part of administrative ethics is examining the epistemology of policy frameworks."
                ),
                baseForm = "Episteme",
                otherForms = "Epistemological",
                relatedForms = "Epistemologist",
                memoryHook = "Episteme = Knowledge in Greek. Epistemology is the study of how we acquire true knowledge.",
                topic = "Philosophical Underpinnings",
                chapter = "Ethics & Integrity"
            ),
            VocabularyWord(
                word = "Anachronism",
                pronunciation = "/əˈnækrəˌnɪzəm/",
                meaning = "A thing belonging or appropriate to a period other than that in which it exists, especially a thing that is conspicuously old-fashioned.",
                examples = listOf(
                    "Using manual registers in digital India is considered an administrative anachronism."
                ),
                baseForm = "Chronology",
                otherForms = "Anachronistic, Anachronisms",
                relatedForms = "Anachronistically",
                memoryHook = "Ana (Against/Wrong) + Chron (Time) = Chronologically misplaced.",
                topic = "E-Governance",
                chapter = "Science & Tech"
            )
        )
        repository.insertWords(demoList)
    }
}
