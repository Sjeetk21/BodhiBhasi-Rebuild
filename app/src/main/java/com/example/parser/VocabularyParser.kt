package com.example.parser

import com.example.model.VocabularyWord

object VocabularyParser {

    /**
     * Parses sequential document lines (paragraphs) into structured vocabulary words using a robust state machine.
     * Handles standard key-value patterns (e.g., "Word: Diligent", "Meaning: Hardworking")
     * and auto-groups content under chapters and topics when encountered.
     */
    fun parseLinesToWords(lines: List<String>): List<VocabularyWord> {
        val words = mutableListOf<VocabularyWord>()
        
        var currentChapter: String? = null
        var currentTopic: String? = null
        
        var tempWord: String? = null
        var tempMeaning: String? = null
        val tempExamples = mutableListOf<String>()
        var tempPronunciation: String? = null
        var tempBaseForm: String? = null
        var tempOtherForms: String? = null
        var tempRelatedForms: String? = null
        var tempMemoryHook: String? = null

        fun buildAndAddWord() {
            val wordVal = tempWord?.trim() ?: return
            val meaningVal = tempMeaning?.trim() ?: "Meaning not found"
            
            words.add(
                VocabularyWord(
                    word = wordVal,
                    meaning = meaningVal,
                    examples = tempExamples.toList(),
                    pronunciation = tempPronunciation?.trim(),
                    baseForm = tempBaseForm?.trim(),
                    otherForms = tempOtherForms?.trim(),
                    relatedForms = tempRelatedForms?.trim(),
                    memoryHook = tempMemoryHook?.trim(),
                    topic = currentTopic,
                    chapter = currentChapter
                )
            )
            
            // Reset builder fields
            tempWord = null
            tempMeaning = null
            tempExamples.clear()
            tempPronunciation = null
            tempBaseForm = null
            tempOtherForms = null
            tempRelatedForms = null
            tempMemoryHook = null
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed == "---") continue

            // Heuristics for Topic/Chapter structural blocks
            if (trimmed.startsWith("Chapter:", ignoreCase = true)) {
                buildAndAddWord() // Build any pending word before changing chapters
                currentChapter = trimmed.substringAfter("Chapter:").trim()
                continue
            }
            if (trimmed.startsWith("Topic:", ignoreCase = true)) {
                buildAndAddWord()
                currentTopic = trimmed.substringAfter("Topic:").trim()
                continue
            }

            // Word Fields Heuristics
            when {
                trimmed.startsWith("Word:", ignoreCase = true) -> {
                    buildAndAddWord() // Commit previous before starting a new one
                    tempWord = trimmed.substringAfter("Word:").trim()
                }
                trimmed.startsWith("Meaning:", ignoreCase = true) -> {
                    tempMeaning = trimmed.substringAfter("Meaning:").trim()
                }
                trimmed.startsWith("Example:", ignoreCase = true) -> {
                    tempExamples.add(trimmed.substringAfter("Example:").trim())
                }
                trimmed.startsWith("Examples:", ignoreCase = true) -> {
                    val exText = trimmed.substringAfter("Examples:").trim()
                    if (exText.contains(";")) {
                        tempExamples.addAll(exText.split(";").map { it.trim() }.filter { it.isNotEmpty() })
                    } else {
                        tempExamples.add(exText)
                    }
                }
                trimmed.startsWith("Pronunciation:", ignoreCase = true) -> {
                    tempPronunciation = trimmed.substringAfter("Pronunciation:").trim()
                }
                trimmed.startsWith("Base Form:", ignoreCase = true) -> {
                    tempBaseForm = trimmed.substringAfter("Base Form:").trim()
                }
                trimmed.startsWith("Other Forms:", ignoreCase = true) -> {
                    tempOtherForms = trimmed.substringAfter("Other Forms:").trim()
                }
                trimmed.startsWith("Related Forms:", ignoreCase = true) -> {
                    tempRelatedForms = trimmed.substringAfter("Related Forms:").trim()
                }
                trimmed.startsWith("Memory Hook:", ignoreCase = true) -> {
                    tempMemoryHook = trimmed.substringAfter("Memory Hook:").trim()
                }
                
                // Fallback smart heuristics if labels are absent but formatted:
                else -> {
                    if (tempWord == null && trimmed.length < 35 && !trimmed.contains(" ") && trimmed.firstOrNull()?.isUpperCase() == true) {
                        buildAndAddWord()
                        tempWord = trimmed
                    } else if (tempWord != null && tempMeaning == null) {
                        // If we have a word but no meaning yet, treat this line as the meaning
                        tempMeaning = trimmed
                    } else if (tempWord != null) {
                        // Fallback add as example or detail
                        if (trimmed.startsWith("-") || trimmed.startsWith("•") || (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains("."))) {
                            tempExamples.add(trimmed.replaceFirst("^[-•\\d.]+".toRegex(), "").trim())
                        }
                    }
                }
            }
        }
        
        // Add final pending word
        buildAndAddWord()
        
        return words
    }
}
