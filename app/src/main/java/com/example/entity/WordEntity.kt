package com.example.entity

import androidx.room.*
import com.example.model.VocabularyWord

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["chapterId"])]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val meaning: String,
    val pronunciation: String?,
    val baseForm: String?,
    val otherForms: String?,
    val relatedForms: String?,
    val memoryHook: String?,
    val topic: String?,
    val chapterId: Int?,
    val dateAdded: Long = System.currentTimeMillis(),
    val acceptedKeywords: String? = null,
    val antonyms: String? = null
)

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["name"], unique = true)]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(
    tableName = "examples",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["wordId"])]
)
data class ExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wordId: Int,
    val example: String
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookmarkEntity(
    @PrimaryKey val wordId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sync_history"
)
data class SyncHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS" or "FAILED"
    val message: String?,
    val wordsAdded: Int,
    val sourceUrl: String
)

@Entity(
    tableName = "view_history",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ViewHistoryEntity(
    @PrimaryKey val wordId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Relation class to map fully normalized tables back into our unified VocabularyWord domain model.
 */
data class WordWithRelations(
    @Embedded val word: WordEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val examples: List<ExampleEntity> = emptyList(),
    
    @Relation(
        parentColumn = "chapterId",
        entityColumn = "id"
    )
    val chapter: ChapterEntity? = null,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val bookmark: BookmarkEntity? = null,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val metadata: VocabularyMetadataEntity? = null
) {
    fun toDomain(): VocabularyWord {
        return VocabularyWord(
            id = word.id,
            word = word.word,
            meaning = word.meaning,
            examples = examples.map { it.example },
            pronunciation = word.pronunciation,
            baseForm = word.baseForm,
            otherForms = word.otherForms,
            relatedForms = word.relatedForms,
            memoryHook = word.memoryHook,
            topic = word.topic,
            chapter = chapter?.name,
            isFavorite = bookmark != null,
            dateAdded = word.dateAdded,
            acceptedKeywords = word.acceptedKeywords,
            antonyms = word.antonyms,
            stability = metadata?.stability ?: 1.0,
            lastRevisedTimestamp = metadata?.lastRevisedTimestamp,
            difficultyFactor = metadata?.difficultyFactor ?: 2.5,
            learningStatus = metadata?.learningStatus ?: "NEW",
            consecutiveFailures = metadata?.consecutiveFailures ?: 0
        )
    }
}

// Keep Converters class for any compile compatibility
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = ""
    @TypeConverter
    fun toStringList(value: String): List<String> = emptyList()
}
