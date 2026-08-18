package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.database.AppDatabase
import com.example.repository.WordRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RevisionTest {

    @Test
    fun testGenerateRevisionSession() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = AppDatabase.getDatabase(context)
        val repository = WordRepository(database.wordDao())
        
        try {
            // Populate database with 512 words
            val words = mutableListOf<com.example.model.VocabularyWord>()
            for (i in 1..512) {
                words.add(
                    com.example.model.VocabularyWord(
                        word = "Word $i",
                        meaning = "Meaning $i",
                        baseForm = "Base $i",
                        chapter = "Chapter $i"
                    )
                )
            }
            repository.insertWords(words)
            println("Imported 512 words.")
            
            val session = repository.generateRevisionSession(System.currentTimeMillis(), 20)
            println("Session generated successfully. Size: ${session.size}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
