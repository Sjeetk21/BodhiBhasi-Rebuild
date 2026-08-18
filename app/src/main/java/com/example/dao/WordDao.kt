package com.example.dao

import androidx.room.*
import com.example.entity.*
import com.example.model.VocabularyWord
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    // --- Core Words Queries ---

    @Transaction
    @Query("SELECT * FROM words ORDER BY word ASC")
    fun getAllWords(): Flow<List<WordWithRelations>>

    @Transaction
    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): WordWithRelations?

    @Transaction
    @Query("SELECT * FROM words WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getWordByName(word: String): WordWithRelations?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    // --- Examples queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamples(examples: List<ExampleEntity>)

    @Query("DELETE FROM examples WHERE wordId = :wordId")
    suspend fun deleteExamplesByWordId(wordId: Int)

    // --- Chapters queries ---

    @Query("SELECT name FROM chapters ORDER BY name ASC")
    fun getAllChapters(): Flow<List<String>>

    @Query("SELECT DISTINCT topic FROM words WHERE topic IS NOT NULL AND topic != '' ORDER BY topic")
    fun getAllSubjects(): Flow<List<String>>

    @Query("SELECT id FROM chapters WHERE name = :name LIMIT 1")
    suspend fun getChapterIdByName(name: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()

    @Transaction
    @Query("""
        SELECT w.* FROM words w
        JOIN chapters c ON w.chapterId = c.id
        WHERE c.name = :chapterName
        ORDER BY w.word ASC
    """)
    fun getWordsByChapter(chapterName: String): Flow<List<WordWithRelations>>

    // --- Category Filters ---
    @Transaction
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN vocabulary_metadata m ON w.id = m.wordId
        WHERE COALESCE(m.learningStatus, 'NEW') = :status
        ORDER BY w.word ASC
    """)
    fun getWordsByStatus(status: String): Flow<List<WordWithRelations>>

    // --- Bookmarks (Favorites) ---

    @Transaction
    @Query("""
        SELECT w.* FROM words w
        JOIN bookmarks b ON w.id = b.wordId
        ORDER BY w.word ASC
    """)
    fun getFavoriteWords(): Flow<List<WordWithRelations>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE wordId = :wordId)")
    suspend fun isBookmarked(wordId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE wordId = :wordId")
    suspend fun deleteBookmark(wordId: Int)

    // --- Search History ---

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearRecentSearches()

    // --- View History ---
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViewHistory(history: com.example.entity.ViewHistoryEntity)
    
    @Transaction
    @Query("SELECT w.* FROM words w INNER JOIN view_history v ON w.id = v.wordId ORDER BY v.timestamp DESC LIMIT 100")
    fun getViewHistory(): Flow<List<WordWithRelations>>
    
    @Query("DELETE FROM view_history")
    suspend fun clearViewHistory()

    // --- FTS Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: WordFtsEntity)

    @Query("DELETE FROM words_fts WHERE rowid = :rowid")
    suspend fun deleteFtsByRowId(rowid: Int)

    @Query("DELETE FROM words_fts")
    suspend fun deleteAllFts()

    @Transaction
    @Query("""
        SELECT w.* FROM words w
        JOIN words_fts f ON w.id = f.rowid
        WHERE words_fts MATCH :query
    """)
    suspend fun searchWordsFts(query: String): List<WordWithRelations>

    @Transaction
    @Query("""
        SELECT * FROM words 
        WHERE word LIKE '%' || :query || '%' 
           OR baseForm LIKE '%' || :query || '%'
           OR relatedForms LIKE '%' || :query || '%'
    """)
    suspend fun searchWordsFallback(query: String): List<WordWithRelations>

    @Transaction
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN chapters c ON w.chapterId = c.id
        WHERE c.name LIKE '%' || :query || '%'
    """)
    suspend fun searchWordsByChapter(query: String): List<WordWithRelations>

    @Transaction
    @Query("""
        SELECT * FROM words 
        WHERE topic LIKE '%' || :query || '%'
    """)
    suspend fun searchWordsBySubject(query: String): List<WordWithRelations>

    // --- Sync History ---

    @Query("SELECT * FROM sync_history ORDER BY timestamp DESC LIMIT 50")
    fun getSyncHistory(): Flow<List<SyncHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncHistory(sync: SyncHistoryEntity)

    // --- Word count statistics ---

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("SELECT COUNT(*) FROM words")
    fun getWordCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bookmarks")
    fun getFavoriteCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE dateAdded >= :startOfDayTimestamp")
    fun getWordsAddedTodayCountFlow(startOfDayTimestamp: Long): Flow<Int>

    @Query("SELECT id FROM words ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomWordId(): Int?

    @Transaction
    suspend fun getRandomWordWithRelations(): WordWithRelations? {
        val id = getRandomWordId() ?: return null
        return getWordsWithRelationsByIds(listOf(id)).firstOrNull()
    }

    @Query("""
        SELECT id FROM words 
        WHERE id NOT IN (:excludeIds)
        ORDER BY RANDOM() 
        LIMIT 1
    """)
    suspend fun getWeightedRandomWordId(excludeIds: List<Int>): Int?

    @Transaction
    suspend fun getWeightedRandomWord(excludeIds: List<Int>): WordWithRelations? {
        val id = getWeightedRandomWordId(excludeIds) ?: return null
        return getWordsWithRelationsByIds(listOf(id)).firstOrNull()
    }

    @Query("SELECT id FROM words ORDER BY RANDOM() LIMIT 1")
    suspend fun getWeightedRandomWordFallbackId(): Int?

    @Transaction
    suspend fun getWeightedRandomWordFallback(): WordWithRelations? {
        val id = getWeightedRandomWordFallbackId() ?: return null
        return getWordsWithRelationsByIds(listOf(id)).firstOrNull()
    }

    // --- Revision Engine Queries ---

    @Query("""
        SELECT w.id FROM words w 
        JOIN vocabulary_metadata m ON w.id = m.wordId 
        WHERE m.learningStatus IN ('LEARNING', 'FAMILIAR') 
          AND (m.lastRevisedTimestamp IS NULL OR m.lastRevisedTimestamp < :excludeRevisedAfter)
        ORDER BY (m.lastRevisedTimestamp + (m.stability * 86400000)) ASC 
        LIMIT :limit
    """)
    suspend fun getDueWordIds(limit: Int, excludeRevisedAfter: Long): List<Int>

    @Query("""
        SELECT w.id FROM words w 
        JOIN vocabulary_metadata m ON w.id = m.wordId 
        WHERE m.learningStatus = 'NEW' 
          AND (m.lastRevisedTimestamp IS NULL OR m.lastRevisedTimestamp < :excludeRevisedAfter)
        ORDER BY w.id ASC 
        LIMIT :limit
    """)
    suspend fun getNewWordIds(limit: Int, excludeRevisedAfter: Long): List<Int>

    @Query("""
        SELECT w.id FROM words w 
        JOIN vocabulary_metadata m ON w.id = m.wordId 
        WHERE m.stability < 2.0 AND m.consecutiveFailures > 2 
          AND (m.lastRevisedTimestamp IS NULL OR m.lastRevisedTimestamp < :excludeRevisedAfter)
        ORDER BY m.stability ASC 
        LIMIT :limit
    """)
    suspend fun getWeakWordIds(limit: Int, excludeRevisedAfter: Long): List<Int>

    @Query("""
        SELECT w.id FROM words w 
        JOIN vocabulary_metadata m ON w.id = m.wordId 
        WHERE m.learningStatus = 'MASTERED' 
          AND (m.lastRevisedTimestamp IS NULL OR m.lastRevisedTimestamp < :excludeRevisedAfter)
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getReinforcementWordIds(limit: Int, excludeRevisedAfter: Long): List<Int>

    @Query("""
        SELECT w.id FROM words w 
        LEFT JOIN vocabulary_metadata m ON w.id = m.wordId 
        WHERE (m.lastRevisedTimestamp IS NULL OR m.lastRevisedTimestamp < :excludeRevisedAfter)
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getAnyRandomWordIds(limit: Int, excludeRevisedAfter: Long): List<Int>

    // --- Stats Queries ---

    @Query("""
        SELECT COUNT(DISTINCT wordId) FROM vocabulary_metadata 
        WHERE lastRevisedTimestamp >= :startOfDay AND lastRevisedTimestamp < :endOfDay
    """)
    suspend fun getWordsRevisedTodayCount(startOfDay: Long, endOfDay: Long): Int

    @Query("""
        SELECT COUNT(*) FROM vocabulary_metadata 
        WHERE learningStatus != 'NEW'
    """)
    suspend fun getWordsLearnedCount(): Int
    
    @Query("""
        SELECT learningStatus, COUNT(*) as count 
        FROM vocabulary_metadata 
        GROUP BY learningStatus
    """)
    suspend fun getLearningStatusCounts(): List<com.example.entity.LearningStatusCount>

    @Query("""
        SELECT lastRevisedTimestamp 
        FROM vocabulary_metadata 
        WHERE lastRevisedTimestamp IS NOT NULL 
        ORDER BY lastRevisedTimestamp DESC
    """)
    suspend fun getAllRevisionTimestamps(): List<Long>

    @Transaction
    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsWithRelationsByIds(ids: List<Int>): List<WordWithRelations>

    @Transaction
    suspend fun getWordsDueForRevision(limit: Int): List<WordWithRelations> {
        val ids = getDueWordIds(limit, 0L)
        if (ids.isEmpty()) return emptyList()
        return getWordsWithRelationsByIds(ids)
    }

    @Transaction
    suspend fun getNewWordsForRevision(limit: Int): List<WordWithRelations> {
        val ids = getNewWordIds(limit, 0L)
        if (ids.isEmpty()) return emptyList()
        return getWordsWithRelationsByIds(ids)
    }

    @Transaction
    suspend fun getWeakWordsForRevision(limit: Int): List<WordWithRelations> {
        val ids = getWeakWordIds(limit, 0L)
        if (ids.isEmpty()) return emptyList()
        return getWordsWithRelationsByIds(ids)
    }

    @Transaction
    suspend fun getRandomReinforcementWords(limit: Int): List<WordWithRelations> {
        val ids = getReinforcementWordIds(limit, 0L)
        if (ids.isEmpty()) return emptyList()
        return getWordsWithRelationsByIds(ids).shuffled()
    }

    @Query("SELECT meaning FROM words WHERE chapterId = (SELECT chapterId FROM words WHERE id = :wordId) AND id != :wordId ORDER BY RANDOM() LIMIT 3")
    suspend fun getDistractorMeanings(wordId: Int): List<String>

    @Query("SELECT meaning FROM words WHERE id != :wordId ORDER BY RANDOM() LIMIT 3")
    suspend fun getDistractorMeaningsFallback(wordId: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocabularyMetadata(metadata: com.example.entity.VocabularyMetadataEntity)

    // --- Atomic Transaction Helpers ---

    @Transaction
    suspend fun toggleBookmark(wordId: Int): Boolean {
        return if (isBookmarked(wordId)) {
            deleteBookmark(wordId)
            false
        } else {
            insertBookmark(BookmarkEntity(wordId = wordId))
            true
        }
    }

    @Transaction
    suspend fun deleteWordWithRelations(wordId: Int) {
        deleteBookmark(wordId)
        deleteExamplesByWordId(wordId)
        deleteFtsByRowId(wordId)
        getWordById(wordId)?.let {
            deleteWord(it.word)
        }
    }

    @Transaction
    suspend fun insertWordWithRelations(word: VocabularyWord): Long {
        val existing = getWordByName(word.word)
        val chapterId = word.chapter?.let { chapterName ->
            getChapterIdByName(chapterName) ?: insertChapter(ChapterEntity(name = chapterName))
        }

        val wordId: Int
        if (existing != null) {
            wordId = existing.word.id
            val wordEntity = WordEntity(
                id = wordId,
                word = word.word,
                meaning = word.meaning,
                pronunciation = word.pronunciation,
                baseForm = word.baseForm,
                otherForms = word.otherForms,
                relatedForms = word.relatedForms,
                memoryHook = word.memoryHook,
                topic = word.topic,
                chapterId = chapterId?.toInt(),
                dateAdded = existing.word.dateAdded
            )
            updateWord(wordEntity)
        } else {
            val wordEntity = WordEntity(
                id = 0,
                word = word.word,
                meaning = word.meaning,
                pronunciation = word.pronunciation,
                baseForm = word.baseForm,
                otherForms = word.otherForms,
                relatedForms = word.relatedForms,
                memoryHook = word.memoryHook,
                topic = word.topic,
                chapterId = chapterId?.toInt(),
                dateAdded = System.currentTimeMillis()
            )
            wordId = insertWord(wordEntity).toInt()
            
            // Initialize metadata for the new word
            insertVocabularyMetadata(com.example.entity.VocabularyMetadataEntity(wordId = wordId))
        }

        // Clear old examples and insert new ones
        deleteExamplesByWordId(wordId)
        val exampleEntities = word.examples.map { ExampleEntity(wordId = wordId, example = it) }
        insertExamples(exampleEntities)

        // Handle Bookmark preservation
        val shouldBeBookmarked = word.isFavorite || (existing?.bookmark != null)
        if (shouldBeBookmarked) {
            insertBookmark(BookmarkEntity(wordId = wordId))
        } else {
            deleteBookmark(wordId)
        }

        // Update FTS
        val ftsEntity = WordFtsEntity(
            rowid = wordId,
            word = word.word,
            meaning = word.meaning,
            examples = word.examples.joinToString("; "),
            baseForm = word.baseForm ?: "",
            otherForms = word.otherForms ?: "",
            relatedForms = word.relatedForms ?: "",
            memoryHook = word.memoryHook ?: "",
            topic = word.topic ?: "",
            chapter = word.chapter ?: ""
        )
        insertFts(ftsEntity)

        return wordId.toLong()
    }

    @Transaction
    suspend fun insertWordsWithRelations(words: List<VocabularyWord>) {
        words.forEach { insertWordWithRelations(it) }
    }

    @Transaction
    suspend fun importWordsWithRelations(words: List<VocabularyWord>): ImportSummary {
        val countBefore = getWordCount()
        var imported = 0
        var updated = 0
        var skipped = 0
        var duplicates = 0
        var bookmarksPreserved = 0
        var ftsUpdated = 0

        words.forEach { word ->
            val existing = getWordByName(word.word)
            if (existing != null) {
                duplicates++
                val shouldBeBookmarked = word.isFavorite || (existing.bookmark != null)
                if (shouldBeBookmarked) {
                    bookmarksPreserved++
                }

                if (isWordIdentical(existing, word)) {
                    skipped++
                } else {
                    // Update
                    val chapterId = word.chapter?.let { chapterName ->
                        getChapterIdByName(chapterName) ?: insertChapter(ChapterEntity(name = chapterName))
                    }
                    val wordEntity = WordEntity(
                        id = existing.word.id,
                        word = word.word,
                        meaning = word.meaning,
                        pronunciation = word.pronunciation,
                        baseForm = word.baseForm,
                        otherForms = word.otherForms,
                        relatedForms = word.relatedForms,
                        memoryHook = word.memoryHook,
                        topic = word.topic,
                        chapterId = chapterId?.toInt(),
                        dateAdded = existing.word.dateAdded,
                        acceptedKeywords = word.acceptedKeywords ?: existing.word.acceptedKeywords,
                        antonyms = word.antonyms ?: existing.word.antonyms
                    )
                    updateWord(wordEntity)

                    deleteExamplesByWordId(existing.word.id)
                    val exampleEntities = word.examples.map { ExampleEntity(wordId = existing.word.id, example = it) }
                    insertExamples(exampleEntities)

                    // Preserve bookmark
                    if (shouldBeBookmarked) {
                        insertBookmark(BookmarkEntity(wordId = existing.word.id))
                    } else {
                        deleteBookmark(existing.word.id)
                    }

                    // Update FTS
                    val ftsEntity = WordFtsEntity(
                        rowid = existing.word.id,
                        word = word.word,
                        meaning = word.meaning,
                        examples = word.examples.joinToString("; "),
                        baseForm = word.baseForm ?: "",
                        otherForms = word.otherForms ?: "",
                        relatedForms = word.relatedForms ?: "",
                        memoryHook = word.memoryHook ?: "",
                        topic = word.topic ?: "",
                        chapter = word.chapter ?: ""
                    )
                    insertFts(ftsEntity)
                    ftsUpdated++

                    updated++
                }
            } else {
                // Insert new
                val chapterId = word.chapter?.let { chapterName ->
                    getChapterIdByName(chapterName) ?: insertChapter(ChapterEntity(name = chapterName))
                }
                val wordEntity = WordEntity(
                    id = 0,
                    word = word.word,
                    meaning = word.meaning,
                    pronunciation = word.pronunciation,
                    baseForm = word.baseForm,
                    otherForms = word.otherForms,
                    relatedForms = word.relatedForms,
                    memoryHook = word.memoryHook,
                    topic = word.topic,
                    chapterId = chapterId?.toInt(),
                    dateAdded = System.currentTimeMillis(),
                    acceptedKeywords = word.acceptedKeywords,
                    antonyms = word.antonyms
                )
                val wordId = insertWord(wordEntity).toInt()

                // Initialize metadata for the new word
                insertVocabularyMetadata(com.example.entity.VocabularyMetadataEntity(wordId = wordId))

                val exampleEntities = word.examples.map { ExampleEntity(wordId = wordId, example = it) }
                insertExamples(exampleEntities)

                if (word.isFavorite) {
                    insertBookmark(BookmarkEntity(wordId = wordId))
                    bookmarksPreserved++
                }

                val ftsEntity = WordFtsEntity(
                    rowid = wordId,
                    word = word.word,
                    meaning = word.meaning,
                    examples = word.examples.joinToString("; "),
                    baseForm = word.baseForm ?: "",
                    otherForms = word.otherForms ?: "",
                    relatedForms = word.relatedForms ?: "",
                    memoryHook = word.memoryHook ?: "",
                    topic = word.topic ?: "",
                    chapter = word.chapter ?: ""
                )
                insertFts(ftsEntity)
                ftsUpdated++

                imported++
            }
        }

        val countAfter = getWordCount()

        // Required logging:
        android.util.Log.d("VocabularyImport", "Database Count Before Import: $countBefore")
        android.util.Log.d("VocabularyImport", "Imported: $imported")
        android.util.Log.d("VocabularyImport", "Updated: $updated")
        android.util.Log.d("VocabularyImport", "Skipped: $skipped")
        android.util.Log.d("VocabularyImport", "Database Count After Import: $countAfter")

        return ImportSummary(
            imported = imported,
            updated = updated,
            skipped = skipped,
            duplicates = duplicates,
            bookmarksPreserved = bookmarksPreserved,
            ftsUpdated = ftsUpdated,
            countBefore = countBefore,
            countAfter = countAfter
        )
    }

    @Transaction
    suspend fun isWordIdentical(existing: WordWithRelations, incoming: VocabularyWord): Boolean {
        if (existing.word.word != incoming.word) return false
        if (existing.word.meaning != incoming.meaning) return false
        if (existing.word.pronunciation != incoming.pronunciation) return false
        if (existing.word.baseForm != incoming.baseForm) return false
        if (existing.word.otherForms != incoming.otherForms) return false
        if (existing.word.relatedForms != incoming.relatedForms) return false
        if (existing.word.memoryHook != incoming.memoryHook) return false
        if (existing.word.topic != incoming.topic) return false
        
        val existingChapter = existing.chapter?.name ?: ""
        val incomingChapter = incoming.chapter ?: ""
        if (existingChapter != incomingChapter) return false

        val existingExamples = existing.examples.map { it.example }.sorted()
        val incomingExamples = incoming.examples.sorted()
        if (existingExamples != incomingExamples) return false

        return true
    }
}

data class ImportSummary(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val duplicates: Int,
    val bookmarksPreserved: Int,
    val ftsUpdated: Int,
    val countBefore: Int,
    val countAfter: Int
)
