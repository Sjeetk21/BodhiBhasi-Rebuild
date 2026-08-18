package com.example.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full Text Search (FTS4) virtual table to support ultra-fast prefix queries
 * across word properties and nested properties.
 */
@Fts4
@Entity(tableName = "words_fts")
data class WordFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int,
    val word: String,
    val meaning: String,
    val examples: String,
    val baseForm: String,
    val otherForms: String,
    val relatedForms: String,
    val memoryHook: String,
    val topic: String,
    val chapter: String
)
