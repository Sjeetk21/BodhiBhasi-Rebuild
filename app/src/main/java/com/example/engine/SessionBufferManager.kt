package com.example.engine

import com.example.model.VocabularyWord

class SessionBufferManager {

    private val sessionQueue = mutableListOf<VocabularyWord>()
    private var questionsAsked = 0
    private var targetSessionSize = 0
    private var uniqueWordsPassed = 0

    // Holds words that were pushed forward but the session ended before we reached them
    // or failed words that we want to offer in the bonus round.
    private val missedWordsBuffer = mutableListOf<VocabularyWord>()

    fun loadSession(words: List<VocabularyWord>, targetSize: Int) {
        sessionQueue.clear()
        sessionQueue.addAll(words)
        targetSessionSize = targetSize
        questionsAsked = 0
        uniqueWordsPassed = 0
        missedWordsBuffer.clear()
    }

    fun getNextWord(): VocabularyWord? {
        if (questionsAsked >= targetSessionSize) {
            // Reached the strict cap of questions
            return null
        }
        return sessionQueue.firstOrNull()
    }

    /**
     * Completes a word successfully.
     */
    fun completeWord() {
        if (sessionQueue.isNotEmpty()) {
            sessionQueue.removeAt(0)
            questionsAsked++
            uniqueWordsPassed++
        }
    }

    /**
     * Processes a failed word. Re-inserts K slots away, and removes the *last unseen word*
     * to ensure the queue doesn't expand past the remaining allowed reviews.
     */
    fun pushWordForward(word: VocabularyWord, displacementIndex: Int = 4) {
        if (sessionQueue.isNotEmpty() && sessionQueue.first().id == word.id) {
            sessionQueue.removeAt(0)
            questionsAsked++
        }
        
        // Evict the last word in the queue to maintain strict session length,
        // ONLY if there are more items in the queue than remaining slots.
        val remainingSlots = targetSessionSize - questionsAsked
        if (sessionQueue.size > remainingSlots && sessionQueue.isNotEmpty()) {
            // Pop the last element (which is an unseen word) to save it for tomorrow
            sessionQueue.removeAt(sessionQueue.size - 1)
        }

        // Now insert the failed word
        val insertIndex = if (sessionQueue.size < displacementIndex) {
            sessionQueue.size
        } else {
            displacementIndex
        }
        sessionQueue.add(insertIndex, word)
        
        // Also keep track of it in the missed words buffer for the end-of-session bonus
        if (!missedWordsBuffer.any { it.id == word.id }) {
            missedWordsBuffer.add(word)
        }
    }
    
    fun getQuestionsAsked(): Int = questionsAsked
    
    fun getUniqueWordsPassed(): Int = uniqueWordsPassed
    
    fun getTargetSessionSize(): Int = targetSessionSize

    fun hasMissedWords(): Boolean {
        return missedWordsBuffer.isNotEmpty()
    }

    /**
     * Launches a bonus session using ONLY the missed words.
     */
    fun startBonusSession() {
        sessionQueue.clear()
        sessionQueue.addAll(missedWordsBuffer)
        targetSessionSize = missedWordsBuffer.size
        questionsAsked = 0
        uniqueWordsPassed = 0
        missedWordsBuffer.clear()
    }
}
