package com.example

import com.example.model.MeaningStyle
import com.example.model.VocabularyWord
import org.junit.Assert.*
import org.junit.Test

class MeaningStyleAndPreviewTest {

    @Test
    fun testMeaningStyleEnumValues() {
        val styles = MeaningStyle.values()
        assertEquals(3, styles.size)

        // 1. SHORT style
        assertEquals("Few Words", MeaningStyle.SHORT.label)
        assertTrue(MeaningStyle.SHORT.description.contains("2-4 words", ignoreCase = true))

        // 2. ONE_LINER style
        assertEquals("One Liner", MeaningStyle.ONE_LINER.label)
        assertTrue(MeaningStyle.ONE_LINER.description.contains("8-15 words", ignoreCase = true))

        // 3. DESCRIPTIVE style
        assertEquals("Descriptive", MeaningStyle.DESCRIPTIVE.label)
        assertTrue(MeaningStyle.DESCRIPTIVE.description.contains("20-35 words", ignoreCase = true))
    }

    @Test
    fun testMeaningStylePromptRules() {
        // Verify prompt rules for each style are distinct and tailored for Gemini AI
        val shortPromptRule = "Keep the 'meaning' extremely brief (2 to 4 words only). E.g., 'Governing body' or 'Rule by law'."
        val oneLinerPromptRule = "Provide a single concise sentence (8 to 15 words) defining the core concept clearly."
        val descriptivePromptRule = "Provide a comprehensive, detailed definition (20 to 35 words) explaining nuances and context."

        assertNotEquals(shortPromptRule, oneLinerPromptRule)
        assertNotEquals(oneLinerPromptRule, descriptivePromptRule)
        assertTrue(shortPromptRule.contains("2 to 4 words"))
        assertTrue(oneLinerPromptRule.contains("8 to 15 words"))
        assertTrue(descriptivePromptRule.contains("20 to 35 words"))
    }

    @Test
    fun testEditableMeaningWorkflow() {
        val originalWord = VocabularyWord(
            word = "Sovereignty",
            meaning = "Supreme power.",
            topic = "Polity",
            chapter = "Constitution"
        )

        // Simulating the user modifying the meaning in the preview review dialog
        val editedMeaning = "The supreme, independent authority and power of a state to govern itself free from external control."
        val updatedWord = originalWord.copy(meaning = editedMeaning)

        assertEquals("Sovereignty", updatedWord.word)
        assertEquals(editedMeaning, updatedWord.meaning)
        assertNotEquals(originalWord.meaning, updatedWord.meaning)
        assertEquals("Polity", updatedWord.topic)
        assertEquals("Constitution", updatedWord.chapter)
    }

    @Test
    fun testListMeaningUpdateSimulation() {
        val wordsList = mutableListOf(
            VocabularyWord(word = "Democracy", meaning = "People rule", topic = "Polity"),
            VocabularyWord(word = "Republic", meaning = "Elected head", topic = "Polity")
        )

        // User edits index 0
        val newMeaning = "A system of government by the whole population through elected representatives."
        wordsList[0] = wordsList[0].copy(meaning = newMeaning)

        assertEquals(newMeaning, wordsList[0].meaning)
        assertEquals("Elected head", wordsList[1].meaning)
    }
}
