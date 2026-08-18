package com.example.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a vocabulary entry in LexiUPSC.
 */
@Serializable
data class VocabularyWord(
    val id: Int = 0,
    val word: String,
    val meaning: String,
    val examples: List<String> = emptyList(),
    val pronunciation: String? = null,
    val baseForm: String? = null,
    val otherForms: String? = null,
    val relatedForms: String? = null,
    val memoryHook: String? = null,
    val topic: String? = null,
    val chapter: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val acceptedKeywords: String? = null,
    val antonyms: String? = null,
    val stability: Double = 1.0,
    val lastRevisedTimestamp: Long? = null,
    val difficultyFactor: Double = 2.5,
    val learningStatus: String = "NEW",
    val consecutiveFailures: Int = 0,
    val searchScore: Float = 0f // Used for FTS ranking
)

sealed class SearchResultItem {
    data class WordMatch(val word: VocabularyWord) : SearchResultItem()
    data class SubjectMatch(val subject: String) : SearchResultItem()
    data class ChapterMatch(val chapter: String) : SearchResultItem()
}

@Serializable
data class AiWordRequest(
    val word: String,
    val context: String? = null,
    val subject: String,
    val chapter: String
)
