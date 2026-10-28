package com.lankeshwar.vaultnote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.SupportSQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, NoteDraftEntity::class,NoteAttachmentEntity::class],
    version = 12,
    exportSchema = false
)
abstract class VaultNoteDatabase: RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteDraftDao(): NoteDraftDao
    abstract fun noteAttachmentDao(): NoteAttachmentDao
    companion object {

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // isPrivate already exists in the version-2 database.
                // No schema change is needed here.
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (
                `noteId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`noteId`, `tagId`)
            )
            """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Merge duplicate tag assignments before removing duplicate tags.
                db.execSQL(
                    """
            INSERT OR IGNORE INTO note_tag_cross_ref (noteId, tagId)
            SELECT ref.noteId, (
                SELECT MIN(t2.id)
                FROM tags t2
                JOIN tags original ON original.id = ref.tagId
                WHERE t2.name = original.name
            )
            FROM note_tag_cross_ref ref
            """.trimIndent()
                )

                // Remove assignments linked to duplicate tag records.
                db.execSQL(
                    """
            DELETE FROM note_tag_cross_ref
            WHERE tagId NOT IN (
                SELECT MIN(id) FROM tags GROUP BY name
            )
            """.trimIndent()
                )

                // Keep one tag record for each name.
                db.execSQL(
                    """
            DELETE FROM tags
            WHERE id NOT IN (
                SELECT MIN(id) FROM tags GROUP BY name
            )
            """.trimIndent()
                )

                // Prevent duplicate tag names.
                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name
            ON tags(name)
            """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE tags
            ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#64B5F6'
            """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE notes
            ADD COLUMN formattedContent TEXT NOT NULL DEFAULT ''
            """.trimIndent()
                )
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS note_drafts (
                draftId TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                formattedContent TEXT NOT NULL,
                isMarkdown INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `note_drafts` (
                `draftId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `formattedContent` TEXT NOT NULL,
                `isMarkdown` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`draftId`)
            )
            """.trimIndent()
                )
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `note_attachments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `noteId` INTEGER NOT NULL,
                `fileName` TEXT NOT NULL,
                `fileType` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
                )
            }
        }


    }
}