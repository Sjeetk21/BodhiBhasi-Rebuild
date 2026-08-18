package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dao.WordDao
import com.example.entity.Converters
import com.example.entity.WordEntity
import com.example.entity.ChapterEntity
import com.example.entity.ExampleEntity
import com.example.entity.BookmarkEntity
import com.example.entity.SearchHistoryEntity
import com.example.entity.SyncHistoryEntity
import com.example.entity.ViewHistoryEntity
import com.example.entity.VocabularyMetadataEntity
import com.example.entity.WordFtsEntity
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        WordEntity::class,
        VocabularyMetadataEntity::class,
        ExampleEntity::class,
        ChapterEntity::class,
        SearchHistoryEntity::class,
        SyncHistoryEntity::class,
        ViewHistoryEntity::class,
        BookmarkEntity::class,
        WordFtsEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create vocabulary_metadata table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `vocabulary_metadata` (
                        `wordId` INTEGER NOT NULL, 
                        `stability` REAL NOT NULL, 
                        `lastRevisedTimestamp` INTEGER, 
                        `difficultyFactor` REAL NOT NULL, 
                        `learningStatus` TEXT NOT NULL, 
                        `consecutiveFailures` INTEGER NOT NULL, 
                        PRIMARY KEY(`wordId`), 
                        FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_scheduling` ON `vocabulary_metadata` (`learningStatus`, `stability`, `lastRevisedTimestamp`)")
                // 1.5 Populate vocabulary_metadata for all existing words
                db.execSQL("""
                    INSERT OR IGNORE INTO vocabulary_metadata (wordId, stability, lastRevisedTimestamp, difficultyFactor, learningStatus, consecutiveFailures)
                    SELECT id, 1.0, NULL, 1.0, 'NEW', 0 FROM words
                """)

                // 2. Recreate words table without the old columns
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `words_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `word` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `pronunciation` TEXT,
                        `baseForm` TEXT,
                        `otherForms` TEXT,
                        `relatedForms` TEXT,
                        `memoryHook` TEXT,
                        `topic` TEXT,
                        `chapterId` INTEGER,
                        `dateAdded` INTEGER NOT NULL,
                        `acceptedKeywords` TEXT,
                        `antonyms` TEXT,
                        FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)

                db.execSQL("""
                    INSERT INTO words_new (id, word, meaning, pronunciation, baseForm, otherForms, relatedForms, memoryHook, topic, chapterId, dateAdded, acceptedKeywords, antonyms)
                    SELECT id, word, meaning, pronunciation, baseForm, otherForms, relatedForms, memoryHook, topic, chapterId, dateAdded, acceptedKeywords, antonyms FROM words
                """)
                
                db.execSQL("DROP TABLE words")
                db.execSQL("ALTER TABLE words_new RENAME TO words")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_chapterId` ON `words` (`chapterId`)")
                
                // Recreate triggers for FTS
                db.execSQL("DROP TRIGGER IF EXISTS words_insert")
                db.execSQL("DROP TRIGGER IF EXISTS words_update")
                db.execSQL("DROP TRIGGER IF EXISTS words_delete")
                
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS words_insert AFTER INSERT ON words BEGIN 
                        INSERT INTO words_fts(rowid, word, meaning) VALUES (new.id, new.word, new.meaning); 
                    END
                """)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS words_update AFTER UPDATE ON words BEGIN 
                        UPDATE words_fts SET word = new.word, meaning = new.meaning WHERE rowid = new.id; 
                    END
                """)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS words_delete AFTER DELETE ON words BEGIN 
                        DELETE FROM words_fts WHERE rowid = old.id; 
                    END
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all words have a metadata record (fixes bug where imported words didn't get metadata)
                db.execSQL("""
                    INSERT OR IGNORE INTO vocabulary_metadata (wordId, stability, lastRevisedTimestamp, difficultyFactor, learningStatus, consecutiveFailures)
                    SELECT id, 1.0, NULL, 1.0, 'NEW', 0 FROM words
                    WHERE id NOT IN (SELECT wordId FROM vocabulary_metadata)
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `view_history` (
                        `wordId` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`wordId`),
                        FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lexi_upsc_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration() // Destructive migration builds new normalized schema instantly
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
