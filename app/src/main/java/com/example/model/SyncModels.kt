package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Step1Report(
    val docId: String,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class Step2Report(
    val exportUrl: String,
    val success: Boolean
)

@Serializable
data class Step3Report(
    val httpCode: Int,
    val contentType: String?,
    val responseSize: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class Step4Report(
    val numLines: Int,
    val numWordsParsed: Int,
    val parserErrors: List<String>,
    val success: Boolean
)

@Serializable
data class Step5Report(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val duplicates: Int,
    val bookmarksPreserved: Int,
    val ftsUpdated: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class SyncPreviewData(
    val step1: Step1Report,
    val step2: Step2Report,
    val step3: Step3Report,
    val step4: Step4Report,
    val chapter: String,
    val topic: String,
    val wordsDetected: Int,
    val firstWord: String,
    val lastWord: String,
    val parsedWords: List<VocabularyWord>
)

@Serializable
data class SyncDebugReport(
    val step1: Step1Report,
    val step2: Step2Report,
    val step3: Step3Report,
    val step4: Step4Report,
    val step5: Step5Report,
    val totalTimeMs: Long
)
