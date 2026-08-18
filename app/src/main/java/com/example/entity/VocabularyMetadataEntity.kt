package com.example.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_metadata",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["learningStatus", "stability", "lastRevisedTimestamp"], name = "idx_scheduling")
    ]
)
data class VocabularyMetadataEntity(
    @PrimaryKey val wordId: Int,
    val stability: Double = 1.0,           // Memory half-life in days
    val lastRevisedTimestamp: Long? = null, // Epoch milli
    val difficultyFactor: Double = 2.5,    // Adjusts based on consistent failures
    val learningStatus: String = "NEW",    // NEW, LEARNING, FAMILIAR, MASTERED
    val consecutiveFailures: Int = 0       // Tracks leech/difficult words
)
