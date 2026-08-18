package com.example.repository

import com.example.dao.WordDao
import com.example.entity.SearchHistoryEntity
import com.example.entity.SyncHistoryEntity
import com.example.model.VocabularyWord
import com.example.model.Step1Report
import com.example.model.Step2Report
import com.example.model.Step3Report
import com.example.model.Step4Report
import com.example.model.Step5Report
import com.example.model.SyncPreviewData
import com.example.model.SyncDebugReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

data class SyncResult(
    val success: Boolean,
    val wordsAdded: Int,
    val imported: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val errorMessage: String? = null
)

class WordRepository(private val wordDao: WordDao) {

    private val httpClient = OkHttpClient()

    val allWords: Flow<List<VocabularyWord>> = wordDao.getAllWords().map { entities ->
        entities.map { it.toDomain() }
    }

    val favoriteWords: Flow<List<VocabularyWord>> = wordDao.getFavoriteWords().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getWordsByStatus(status: String): Flow<List<VocabularyWord>> = wordDao.getWordsByStatus(status).map { entities ->
        entities.map { it.toDomain() }
    }

    val recentSearches: Flow<List<String>> = wordDao.getRecentSearches().map { entities ->
        entities.map { it.query }
    }

    val viewHistory: Flow<List<VocabularyWord>> = wordDao.getViewHistory().map { entities ->
        entities.map { it.toDomain() }
    }

    val allChapters: Flow<List<String>> = wordDao.getAllChapters()
    val allSubjects: Flow<List<String>> = wordDao.getAllSubjects()

    val totalWordsCount: Flow<Int> = wordDao.getWordCountFlow()
    val favoriteWordsCount: Flow<Int> = wordDao.getFavoriteCountFlow()
    val syncHistory: Flow<List<SyncHistoryEntity>> = wordDao.getSyncHistory()

    fun getWordsByChapter(chapter: String): Flow<List<VocabularyWord>> {
        return wordDao.getWordsByChapter(chapter).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getWordsAddedTodayCount(startOfDay: Long): Flow<Int> {
        return wordDao.getWordsAddedTodayCountFlow(startOfDay)
    }

    suspend fun getWordById(id: Int): VocabularyWord? {
        val entity = wordDao.getWordById(id) ?: return null
        // View count tracking removed as per spec
        return entity.toDomain()
    }

    suspend fun toggleFavorite(wordId: Int): Boolean {
        return wordDao.toggleBookmark(wordId)
    }

    suspend fun getRandomWord(): VocabularyWord? {
        return wordDao.getRandomWordWithRelations()?.toDomain()
    }

    /**
     * Weighted random selection that prioritizes unseen/less-seen words
     * and avoids recently shown word IDs.
     */
    suspend fun getWeightedRandomWord(excludeIds: List<Int>): VocabularyWord? {
        // Try weighted random excluding recently shown
        val result = if (excludeIds.isNotEmpty()) {
            wordDao.getWeightedRandomWord(excludeIds)
                ?: wordDao.getWeightedRandomWordFallback() // Fallback if all excluded
        } else {
            wordDao.getWeightedRandomWordFallback()
        }
        return result?.toDomain()
    }

    suspend fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            wordDao.insertRecentSearch(SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun deleteRecentSearch(query: String) {
        wordDao.deleteRecentSearch(query)
    }

    suspend fun clearRecentSearches() {
        wordDao.clearRecentSearches()
    }

    suspend fun clearViewHistory() {
        wordDao.clearViewHistory()
    }
    
    suspend fun recordWordView(wordId: Int) {
        wordDao.insertViewHistory(com.example.entity.ViewHistoryEntity(wordId = wordId))
    }

    suspend fun insertWord(word: VocabularyWord): Long {
        return wordDao.insertWordWithRelations(word)
    }

    suspend fun getWordCount(): Int {
        return wordDao.getWordCount()
    }

    suspend fun insertWords(words: List<VocabularyWord>) {
        wordDao.importWordsWithRelations(words)
    }

    suspend fun deleteWord(word: VocabularyWord) {
        wordDao.deleteWordWithRelations(word.id)
    }

    suspend fun deleteAllWords() {
        wordDao.deleteAllWords()
        wordDao.deleteAllFts()
        wordDao.deleteAllChapters()
    }

    // --- Revision Engine Methods ---

    suspend fun getWordsDueForRevision(limit: Int): List<VocabularyWord> {
        return wordDao.getWordsDueForRevision(limit).map { it.toDomain() }
    }

    suspend fun getNewWordsForRevision(limit: Int): List<VocabularyWord> {
        return wordDao.getNewWordsForRevision(limit).map { it.toDomain() }
    }

    suspend fun getWeakWordsForRevision(limit: Int): List<VocabularyWord> {
        return wordDao.getWeakWordsForRevision(limit).map { it.toDomain() }
    }

    suspend fun getRandomReinforcementWords(limit: Int): List<VocabularyWord> {
        return wordDao.getRandomReinforcementWords(limit).map { it.toDomain() }
    }

    suspend fun generateRevisionSession(todayTime: Long, targetSize: Int = 20, excludeRevisedAfter: Long = 0L): List<VocabularyWord> {
        val slotAllocator = com.example.engine.SlotAllocator(wordDao)
        return slotAllocator.allocateDailySession(targetSize, excludeRevisedAfter)
    }

    suspend fun getDistractorMeanings(wordId: Int): List<String> {
        val distractors = wordDao.getDistractorMeanings(wordId)
        if (distractors.size < 3) {
            val fallback = wordDao.getDistractorMeaningsFallback(wordId)
            return (distractors + fallback).distinct().take(3)
        }
        return distractors
    }

    suspend fun updateWordMetadata(wordId: Int, stability: Double, lastRevisedTimestamp: Long?, difficultyFactor: Double, learningStatus: String, consecutiveFailures: Int) {
        val metadata = com.example.entity.VocabularyMetadataEntity(
            wordId = wordId,
            stability = stability,
            lastRevisedTimestamp = lastRevisedTimestamp,
            difficultyFactor = difficultyFactor,
            learningStatus = learningStatus,
            consecutiveFailures = consecutiveFailures
        )
        wordDao.insertVocabularyMetadata(metadata)
    }

    suspend fun getWordsRevisedTodayCount(startOfDay: Long, endOfDay: Long): Int {
        return wordDao.getWordsRevisedTodayCount(startOfDay, endOfDay)
    }

    suspend fun getWordsLearnedCount(): Int {
        return wordDao.getWordsLearnedCount()
    }

    suspend fun getAllRevisionTimestamps(): List<Long> {
        return wordDao.getAllRevisionTimestamps()
    }
    
    suspend fun getLearningStatusCounts(): List<com.example.entity.LearningStatusCount> {
        return wordDao.getLearningStatusCounts()
    }

    /**
     * Performs Google Doc synchronization by extracting the document ID automatically,
     * downloading the public TXT export, parsing, and executing an upsert-with-relations
     * database transaction which outputs before/after stats, imported, updated, and skipped.
     */
    /**
     * Step-by-step synchronization preview. Fetches and parses document in memory without writing to database.
     */
    suspend fun previewGoogleDocSync(urlOrId: String): SyncPreviewData {
        val startTime = System.currentTimeMillis()
        
        // Step 1: Extract Document ID
        val docId = if (urlOrId.isNotBlank()) {
            SettingsRepository.extractGoogleDocId(urlOrId)
        } else {
            ""
        }
        val step1Success = docId.isNotBlank() && docId.matches("^[a-zA-Z0-9-_]+$".toRegex())
        val step1 = Step1Report(
            docId = docId,
            success = step1Success,
            errorMessage = if (step1Success) null else "Invalid or empty Google Document link or ID format."
        )

        // Step 2: Construct Export URL
        val exportUrl = if (step1Success) {
            if (urlOrId.contains("/spreadsheets/") || urlOrId.contains("spreadsheet") || !urlOrId.startsWith("http")) {
                "https://docs.google.com/spreadsheets/d/$docId/export?format=csv"
            } else {
                "https://docs.google.com/document/d/$docId/export?format=txt"
            }
        } else {
            ""
        }
        val step2 = Step2Report(
            exportUrl = exportUrl,
            success = step1Success
        )

        if (!step1Success) {
            return SyncPreviewData(
                step1 = step1,
                step2 = step2,
                step3 = Step3Report(0, null, 0, false, "Failed at Step 1: Invalid Document ID"),
                step4 = Step4Report(0, 0, listOf("Failed at previous step"), false),
                chapter = "Unknown",
                topic = "Unknown",
                wordsDetected = 0,
                firstWord = "None",
                lastWord = "None",
                parsedWords = emptyList()
            )
        }

        // Step 3: HTTP Request
        val request = Request.Builder().url(exportUrl).build()
        var httpCode = 0
        var contentType: String? = null
        var responseSize = 0L
        var responseBody: String? = null
        var step3Success = false
        var step3Error: String? = null

        try {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                httpClient.newCall(request).execute().use { response ->
                    httpCode = response.code
                    contentType = response.header("Content-Type")
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        responseSize = bytes?.size?.toLong() ?: 0L
                        responseBody = bytes?.let { String(it, Charsets.UTF_8) }
                        step3Success = responseBody != null
                        if (!step3Success) {
                            step3Error = "Response body is empty from document."
                        }
                    } else {
                        step3Error = "HTTP Error: ${response.code}. Ensure the document is public (Anyone with link can view)."
                    }
                }
            }
        } catch (e: Exception) {
            step3Success = false
            step3Error = e.localizedMessage ?: "Unknown network request error"
        }

        val step3 = Step3Report(
            httpCode = httpCode,
            contentType = contentType,
            responseSize = responseSize,
            success = step3Success,
            errorMessage = step3Error
        )

        if (!step3Success || responseBody == null) {
            return SyncPreviewData(
                step1 = step1,
                step2 = step2,
                step3 = step3,
                step4 = Step4Report(0, 0, listOf("Failed at previous step"), false),
                chapter = "Unknown",
                topic = "Unknown",
                wordsDetected = 0,
                firstWord = "None",
                lastWord = "None",
                parsedWords = emptyList()
            )
        }

        // Step 4: Parser
        val lines = responseBody!!.lines()
        val numLines = lines.size
        var step4Success = false
        val parserErrors = mutableListOf<String>()
        var parsedWords = emptyList<VocabularyWord>()

        try {
            parsedWords = if (exportUrl.contains("spreadsheets")) {
                com.example.parser.SpreadsheetParser.parseCsvToWords(responseBody!!)
            } else {
                com.example.parser.VocabularyParser.parseLinesToWords(lines)
            }
            step4Success = parsedWords.isNotEmpty()
            if (!step4Success) {
                parserErrors.add("No valid vocabulary structures found in downloaded document.")
            }
        } catch (e: Exception) {
            step4Success = false
            parserErrors.add(e.localizedMessage ?: "Unknown parsing error")
        }

        val step4 = Step4Report(
            numLines = numLines,
            numWordsParsed = parsedWords.size,
            parserErrors = parserErrors,
            success = step4Success
        )

        val chapterName = parsedWords.firstOrNull { !it.chapter.isNullOrBlank() }?.chapter ?: "Not specified"
        val topicName = parsedWords.firstOrNull { !it.topic.isNullOrBlank() }?.topic ?: "Not specified"
        val firstWord = parsedWords.firstOrNull()?.word ?: "None"
        val lastWord = parsedWords.lastOrNull()?.word ?: "None"

        return SyncPreviewData(
            step1 = step1,
            step2 = step2,
            step3 = step3,
            step4 = step4,
            chapter = chapterName,
            topic = topicName,
            wordsDetected = parsedWords.size,
            firstWord = firstWord,
            lastWord = lastWord,
            parsedWords = parsedWords
        )
    }

    /**
     * Saves parsed words to the database. Outputs Step 5 detailed reports.
     */
    suspend fun importParsedGoogleDocWords(
        parsedWords: List<VocabularyWord>,
        step1: Step1Report,
        step2: Step2Report,
        step3: Step3Report,
        step4: Step4Report,
        startTimeMs: Long
    ): SyncDebugReport {
        var step5Success = false
        var step5Error: String? = null
        var summary = com.example.dao.ImportSummary(0, 0, 0, 0, 0, 0, 0, 0)

        try {
            if (parsedWords.isNotEmpty()) {
                summary = wordDao.importWordsWithRelations(parsedWords)
                step5Success = true
            } else {
                step5Error = "No parsed words to import"
            }
        } catch (e: Exception) {
            step5Success = false
            step5Error = e.localizedMessage ?: "Unknown database write error"
        }

        val step5 = Step5Report(
            imported = summary.imported,
            updated = summary.updated,
            skipped = summary.skipped,
            duplicates = summary.duplicates,
            bookmarksPreserved = summary.bookmarksPreserved,
            ftsUpdated = summary.ftsUpdated,
            success = step5Success,
            errorMessage = step5Error
        )

        // Record in SyncHistory
        val historyEntry = SyncHistoryEntity(
            timestamp = System.currentTimeMillis(),
            status = if (step5Success) "SUCCESS" else "FAILED",
            message = step5Error,
            wordsAdded = summary.imported,
            sourceUrl = step2.exportUrl
        )
        wordDao.insertSyncHistory(historyEntry)

        return SyncDebugReport(
            step1 = step1,
            step2 = step2,
            step3 = step3,
            step4 = step4,
            step5 = step5,
            totalTimeMs = System.currentTimeMillis() - startTimeMs
        )
    }

    suspend fun syncFromGoogleDocs(urlOrId: String): SyncResult {
        val startTime = System.currentTimeMillis()
        val preview = previewGoogleDocSync(urlOrId)
        if (!preview.step4.success) {
            val errMsg = preview.step1.errorMessage ?: preview.step3.errorMessage ?: preview.step4.parserErrors.firstOrNull() ?: "Synchronization failed"
            return SyncResult(success = false, wordsAdded = 0, errorMessage = errMsg)
        }
        val report = importParsedGoogleDocWords(preview.parsedWords, preview.step1, preview.step2, preview.step3, preview.step4, startTime)
        return if (report.step5.success) {
            SyncResult(
                success = true,
                wordsAdded = report.step5.imported,
                imported = report.step5.imported,
                updated = report.step5.updated,
                skipped = report.step5.skipped,
                failed = 0
            )
        } else {
            SyncResult(
                success = false,
                wordsAdded = 0,
                errorMessage = report.step5.errorMessage
            )
        }
    }

    /**
     * Executes Full Text Search across all attributes and ranks the results
     * as requested: Exact word match -> Word starts with -> Word contains -> Forms -> Meaning -> Examples
     */
    suspend fun searchAndRank(rawQuery: String, scope: com.example.state.SearchScope = com.example.state.SearchScope.WORDS): List<com.example.model.SearchResultItem> {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lowercaseQuery = trimmed.lowercase(Locale.getDefault())

        when (scope) {
            com.example.state.SearchScope.SUBJECT -> {
                return allSubjects.first()
                    .filter { it.lowercase(Locale.getDefault()).contains(lowercaseQuery) }
                    .sortedBy { if (it.lowercase(Locale.getDefault()).startsWith(lowercaseQuery)) 0 else 1 }
                    .map { com.example.model.SearchResultItem.SubjectMatch(it) }
            }
            com.example.state.SearchScope.CHAPTER -> {
                return allChapters.first()
                    .filter { it.lowercase(Locale.getDefault()).contains(lowercaseQuery) }
                    .sortedBy { if (it.lowercase(Locale.getDefault()).startsWith(lowercaseQuery)) 0 else 1 }
                    .map { com.example.model.SearchResultItem.ChapterMatch(it) }
            }
            com.example.state.SearchScope.WORDS -> {
                val rawMatches = try {
                    val terms = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    val ftsQuery = terms.joinToString(" AND ") { term -> "(word:$term* OR baseForm:$term* OR relatedForms:$term*)" }
                    val matches = wordDao.searchWordsFts(ftsQuery)
                    if (matches.isEmpty()) {
                        wordDao.searchWordsFallback(trimmed)
                    } else {
                        matches
                    }
                } catch (e: Exception) {
                    wordDao.searchWordsFallback(trimmed)
                }

                val rankedList = rawMatches.map { entity ->
                    val domainWord = entity.toDomain()
                    var score = 0f

                    val wordLower = domainWord.word.lowercase(Locale.getDefault())
                    val baseLower = domainWord.baseForm?.lowercase(Locale.getDefault()) ?: ""
                    val relatedLower = domainWord.relatedForms?.lowercase(Locale.getDefault()) ?: ""
                    
                    if (wordLower == lowercaseQuery) score += 100f
                    else if (wordLower.startsWith(lowercaseQuery)) score += 80f
                    else if (wordLower.contains(lowercaseQuery)) score += 60f

                    if (baseLower == lowercaseQuery) score += 50f
                    else if (baseLower.startsWith(lowercaseQuery)) score += 40f
                    else if (baseLower.contains(lowercaseQuery)) score += 20f

                    if (relatedLower.contains(lowercaseQuery)) score += 10f

                    domainWord.copy(searchScore = score)
                }
                .filter { it.searchScore > 0f || trimmed.isEmpty() }
                .sortedWith(
                    compareByDescending<VocabularyWord> { it.searchScore }
                        .thenBy { it.word }
                )

                return rankedList.map { com.example.model.SearchResultItem.WordMatch(it) }
            }
        }
    }

    suspend fun generateWordsWithAi(
        requests: List<com.example.model.AiWordRequest>,
        appsScriptUrl: String,
        meaningStyle: com.example.model.MeaningStyle = com.example.model.MeaningStyle.SHORT
    ): List<VocabularyWord> {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Gemini API key is not configured. Please add your GEMINI_API_KEY to the .env file.")
        }
        
        val wordsDescriptions = requests.joinToString("\n") { req ->
            val ctx = if (!req.context.isNullOrBlank()) ", Context = \"${req.context}\"" else ""
            "- Word: \"${req.word}\", Subject: \"${req.subject}\", Chapter: \"${req.chapter}\"$ctx"
        }

        val meaningRule = when (meaningStyle) {
            com.example.model.MeaningStyle.SHORT -> 
                "- \"Meaning\": Prefer a VERY SHORT concise phrase (2-4 words max). Capture only the direct essence or punchy synonym. E.g., \"Came to power\" or \"Extreme scarcity\". Do NOT write long sentences."
            com.example.model.MeaningStyle.ONE_LINER -> 
                "- \"Meaning\": One clear, crisp, single-line sentence (8-15 words max) in plain English explaining the exact definition clearly."
            com.example.model.MeaningStyle.DESCRIPTIVE -> 
                "- \"Meaning\": A comprehensive, descriptive definition (20-35 words) providing in-depth conceptual clarity, nuance, and academic depth suitable for UPSC examinations."
        }

        val prompt = """
            You are an expert vocabulary assistant for a UPSC exam app. Generate word details for the following words:
            $wordsDescriptions
            
            CRITICAL STYLE RULES:
            $meaningRule
            - "Example": Provide ONE simple sentence (10-20 words) per example in the context of the subject/chapter.
            - "Memory Hook": 2-4 words max. E.g., "Rise to power"
            - "Related Forms": Comma-separated word forms only.
            - "Accepted Keywords": Generate 5–10 semantically meaningful keywords or short phrases that would reasonably be accepted as correct meanings during revision. Avoid repeating the exact official meaning verbatim. Include common synonyms and paraphrases where appropriate.
            - "Antonyms": Comma-separated opposites.
            - "Pronunciation": Simple phonetic pronunciation preferred (e.g., "uh-SEN-did"). Avoid complex IPA.
            - Use simple, everyday English. Avoid unnecessary fluff.
            - Keep the chapter and subject EXACTLY as specified for each word.
            
            Output ONLY a valid JSON array containing an object for each word. Example:
            [
              {
                "Word": "FirstWord",
                "Meaning": "Definition according to requested style rule",
                "Example 1": "Simple example sentence in context of the specified chapter",
                "Example 2": "Another simple example sentence",
                "Example 3": "Another simple example sentence",
                "Memory Hook": "2-4 word memory aid",
                "Base Form": "Root form of the word",
                "Subject": "The subject specified for this word",
                "Chapter": "The chapter specified for this word",
                "Related Forms": "Comma-separated word forms",
                "Accepted Keywords": "Comma-separated synonyms and accepted phrases",
                "Antonyms": "Comma-separated antonyms",
                "Pronunciation": "Simple phonetic spelling"
              }
            ]
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val parsedWords = mutableListOf<VocabularyWord>()
        
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to generate content: ${response.message}")
                }
                val resBody = response.body?.string() ?: throw Exception("Empty response from AI model")
                val jsonRes = JSONObject(resBody)
                val candidates = jsonRes.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                var textResult = parts.getJSONObject(0).getString("text").trim()
                
                // Clean up markdown formatting if present
                if (textResult.startsWith("```json")) {
                    textResult = textResult.removePrefix("```json").removeSuffix("```").trim()
                } else if (textResult.startsWith("```")) {
                    textResult = textResult.removePrefix("```").removeSuffix("```").trim()
                }
                
                val jsonArray = if (textResult.startsWith("[")) {
                    JSONArray(textResult)
                } else {
                    val obj = JSONObject(textResult)
                    JSONArray().put(obj)
                }
                
                for (i in 0 until jsonArray.length()) {
                    val wordJson = jsonArray.getJSONObject(i)
                    
                    val example1 = wordJson.optString("Example 1", "")
                    val example2 = wordJson.optString("Example 2", "")
                    val example3 = wordJson.optString("Example 3", "")
                    val examplesList = listOfNotNull(
                        example1.takeIf { it.isNotBlank() },
                        example2.takeIf { it.isNotBlank() },
                        example3.takeIf { it.isNotBlank() }
                    )
                    
                    parsedWords.add(VocabularyWord(
                        word = wordJson.optString("Word", "").trim(),
                        meaning = wordJson.optString("Meaning", "").trim(),
                        examples = examplesList,
                        memoryHook = wordJson.optString("Memory Hook", "").trim(),
                        baseForm = wordJson.optString("Base Form", "").trim(),
                        topic = wordJson.optString("Subject", "").trim(),
                        chapter = wordJson.optString("Chapter", "").trim(),
                        relatedForms = wordJson.optString("Related Forms", "").trim(),
                        acceptedKeywords = wordJson.optString("Accepted Keywords", "").trim(),
                        antonyms = wordJson.optString("Antonyms", "").trim(),
                        pronunciation = wordJson.optString("Pronunciation", "").trim()
                    ))
                }
            }
        }
        
        return parsedWords
    }

    suspend fun saveWordsToDbAndSheet(words: List<VocabularyWord>, appsScriptUrl: String) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            for (word in words) {
                // Insert to local DB
                val generatedId = wordDao.insertWordWithRelations(word)
                val finalWord = word.copy(id = generatedId.toInt())

                // Post to Google Sheet if Web App URL is set
                if (appsScriptUrl.isNotBlank()) {
                    val sheetBody = JSONObject().apply {
                        put("word", finalWord.word)
                        put("meaning", finalWord.meaning)
                        put("example1", finalWord.examples.getOrNull(0) ?: "")
                        put("example2", finalWord.examples.getOrNull(1) ?: "")
                        put("example3", finalWord.examples.getOrNull(2) ?: "")
                        put("memoryHook", finalWord.memoryHook ?: "")
                        put("baseForm", finalWord.baseForm ?: "")
                        put("subject", finalWord.topic ?: "")
                        put("chapter", finalWord.chapter ?: "")
                        put("relatedForms", finalWord.relatedForms ?: "")
                        put("antonyms", finalWord.antonyms ?: "")
                        put("acceptedKeywords", finalWord.acceptedKeywords ?: "")
                        put("pronunciation", finalWord.pronunciation ?: "")
                    }

                    val sheetRequest = Request.Builder()
                        .url(appsScriptUrl)
                        .post(sheetBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    try {
                        httpClient.newCall(sheetRequest).execute().use { response ->
                            if (!response.isSuccessful) {
                                throw Exception("Google Sheets error: HTTP ${response.code}")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw Exception("Failed to sync word '${finalWord.word}' to Google Sheets: ${e.localizedMessage}")
                    }
                }
            }
        }
    }
}
