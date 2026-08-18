package com.example.engine

import com.example.dao.WordDao
import com.example.model.VocabularyWord

class SlotAllocator(private val wordDao: WordDao) {

    /**
     * Allocates a daily session of N words following the cognitive constraints:
     * 10% Weak, 60% Overdue, 20% New, 10% Random Reinforcement.
     * Iteratively fills from buckets to guarantee exact session size if DB has enough words.
     */
    suspend fun allocateDailySession(targetSessionSize: Int = 20, excludeRevisedAfter: Long = 0L): List<VocabularyWord> {
        val sessionIds = mutableSetOf<Int>()
        val sessionWords = mutableListOf<VocabularyWord>()

        // Helper to add distinct words to our session
        suspend fun fillFrom(fetch: suspend (Int) -> List<Int>, count: Int) {
            if (count <= 0) return
            // We fetch up to count + size of sessionIds just in case they overlap,
            // but fetching a bit more gives us buffer.
            val ids = fetch(count + sessionIds.size)
            for (id in ids) {
                if (sessionIds.size >= targetSessionSize) break
                if (sessionIds.add(id)) {
                    val word = wordDao.getWordsWithRelationsByIds(listOf(id)).firstOrNull()?.toDomain()
                    if (word != null) {
                        sessionWords.add(word)
                    }
                }
            }
        }

        // 1. Calculate Target Allocations
        val weakTarget = (targetSessionSize * 0.1).toInt()
        val overdueTarget = (targetSessionSize * 0.6).toInt()
        val newTarget = (targetSessionSize * 0.2).toInt()
        val randomTarget = (targetSessionSize * 0.1).toInt() // Or just targetSessionSize - weak - overdue - new

        // 2. Weak Cohort
        fillFrom({ wordDao.getWeakWordIds(it, excludeRevisedAfter) }, weakTarget)
        
        // 3. Overdue (Learning/Familiar) Cohort
        val remainingOverdueTarget = overdueTarget + (weakTarget - sessionIds.size).coerceAtLeast(0)
        fillFrom({ wordDao.getDueWordIds(it, excludeRevisedAfter) }, remainingOverdueTarget)

        // 4. New Words Cohort
        val remainingNewTarget = newTarget + (overdueTarget + weakTarget - sessionIds.size).coerceAtLeast(0)
        fillFrom({ wordDao.getNewWordIds(it, excludeRevisedAfter) }, remainingNewTarget)

        // 5. Random Reinforcement Cohort (Mastered)
        val remainingRandomTarget = targetSessionSize - sessionIds.size
        fillFrom({ wordDao.getReinforcementWordIds(it, excludeRevisedAfter) }, remainingRandomTarget)

        // 6. Final fallback: If we still don't have enough words, just pull from anywhere (e.g. anything not mastered, or any word)
        if (sessionIds.size < targetSessionSize) {
            val missing = targetSessionSize - sessionIds.size
            // Use the reinforcement as a fallback to pull ANY random word since they are shuffled.
            // Wait, we need a query to get ANY random word ids to fill the session perfectly.
            val fallbackIds = wordDao.getAnyRandomWordIds(missing + sessionIds.size, excludeRevisedAfter)
            for (id in fallbackIds) {
                if (sessionIds.size >= targetSessionSize) break
                if (sessionIds.add(id)) {
                    val word = wordDao.getWordsWithRelationsByIds(listOf(id)).firstOrNull()?.toDomain()
                    if (word != null) sessionWords.add(word)
                }
            }
        }

        return sessionWords.take(targetSessionSize)
    }
}
