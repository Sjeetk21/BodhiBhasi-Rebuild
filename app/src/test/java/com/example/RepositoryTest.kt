package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dao.WordDao
import com.example.database.AppDatabase
import com.example.model.VocabularyWord
import com.example.repository.WordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WordDao
    private lateinit var repository: WordRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wordDao()
        repository = WordRepository(dao)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveWords() = runBlocking {
        val word = VocabularyWord(
            word = "Hegemony",
            pronunciation = "/hɪˈdʒɛməni/",
            meaning = "Dominance, influence, or authority over others.",
            examples = listOf("cultural hegemony"),
            baseForm = "Hegemon",
            otherForms = "Hegemonic",
            relatedForms = "Hegemonist",
            memoryHook = "Huge Money",
            topic = "International Relations",
            chapter = "Polity"
        )

        repository.insertWords(listOf(word))
        
        val allWords = repository.allWords.first()
        assertEquals(1, allWords.size)
        assertEquals("Hegemony", allWords[0].word)
        assertEquals("Polity", allWords[0].chapter)
    }

    @Test
    fun testBookmarkingAndFavorites() = runBlocking {
        val word = VocabularyWord(
            word = "Sovereignty",
            pronunciation = "/ˈsɒvrənti/",
            meaning = "Supreme power or authority.",
            examples = listOf("State sovereignty"),
            baseForm = "Sovereign",
            otherForms = "Sovereigns",
            relatedForms = "Sovereignty",
            memoryHook = "Absolute reign",
            topic = "Constitutional Law",
            chapter = "Polity"
        )

        repository.insertWords(listOf(word))
        val insertedWord = repository.allWords.first()[0]
        
        assertFalse(insertedWord.isFavorite)

        // Toggle Favorite
        repository.toggleFavorite(insertedWord.id)
        
        val favoriteWords = repository.favoriteWords.first()
        assertEquals(1, favoriteWords.size)
        assertTrue(favoriteWords[0].isFavorite)
    }

    @Test
    fun testRecentSearchQueries() = runBlocking {
        repository.addRecentSearch("Federalism")
        repository.addRecentSearch("Sovereignty")

        val recent = repository.recentSearches.first()
        assertEquals(2, recent.size)
        assertEquals("Sovereignty", recent[0]) // Latest should be first
    }
}
