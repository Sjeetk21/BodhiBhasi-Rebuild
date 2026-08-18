package com.example.engine

import java.util.Locale

object LocalAnswerEvaluator {

    enum class EvaluationLevel {
        CORRECT,
        PARTIALLY_CORRECT,
        INCORRECT
    }

    private val stopWords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "because", "as", "what",
        "when", "where", "how", "why", "who", "which", "this", "that", "these",
        "those", "is", "am", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "to", "of", "in", "for",
        "on", "with", "at", "by", "from", "up", "about", "into", "over", "after"
    )

    fun evaluate(
        userAnswer: String,
        officialMeaning: String,
        acceptedKeywords: String?
    ): EvaluationLevel {
        val userTokens = processText(userAnswer)
        if (userTokens.isEmpty()) return EvaluationLevel.INCORRECT

        val officialTokens = processText(officialMeaning)
        val keywordTokens = processText(acceptedKeywords ?: "")

        val targetTokens = (officialTokens + keywordTokens).toSet()

        if (targetTokens.isEmpty()) {
            return EvaluationLevel.INCORRECT // Shouldn't happen in valid DB
        }

        // Calculate overlap
        var matchCount = 0
        for (uToken in userTokens) {
            // Check for exact match or fuzzy match (e.g., stemming overlap)
            val matched = targetTokens.any { tToken ->
                uToken == tToken || 
                (uToken.length > 3 && tToken.length > 3 && (uToken.startsWith(tToken) || tToken.startsWith(uToken)))
            }
            if (matched) {
                matchCount++
            }
        }

        // Calculate scores
        val coverage = matchCount.toFloat() / Math.max(1, Math.min(userTokens.size, targetTokens.size)).toFloat()

        return when {
            coverage >= 0.75f -> EvaluationLevel.CORRECT
            coverage >= 0.40f -> EvaluationLevel.PARTIALLY_CORRECT
            else -> EvaluationLevel.INCORRECT
        }
    }

    private fun processText(text: String): Set<String> {
        if (text.isBlank()) return emptySet()

        // 1 & 2. Lowercase and trim
        var processed = text.trim().lowercase(Locale.getDefault())

        // 3. Remove punctuation
        processed = processed.replace(Regex("[^a-z0-9\\s]"), " ")

        // 6. Split into keywords
        val rawTokens = processed.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // 4 & 5. Remove stop words and stem
        return rawTokens
            .filter { !stopWords.contains(it) }
            .map { simpleStem(it) }
            .toSet()
    }

    private fun simpleStem(word: String): String {
        if (word.length <= 3) return word
        
        var stemmed = word
        if (stemmed.endsWith("ing")) {
            stemmed = stemmed.removeSuffix("ing")
        } else if (stemmed.endsWith("ed")) {
            stemmed = stemmed.removeSuffix("ed")
        } else if (stemmed.endsWith("s") && !stemmed.endsWith("ss")) {
            stemmed = stemmed.removeSuffix("s")
        }
        
        return if (stemmed.length >= 3) stemmed else word
    }
}
