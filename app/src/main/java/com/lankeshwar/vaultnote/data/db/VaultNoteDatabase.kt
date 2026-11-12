package com.lankeshwar.vaultnote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lankeshwar.vaultnote.data.db.dao.JournalDao
import com.lankeshwar.vaultnote.data.db.dao.NoteAttachmentDao
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.db.dao.NoteDraftDao
import com.lankeshwar.vaultnote.data.db.dao.TagDao
import com.lankeshwar.vaultnote.data.db.dao.TaskDao
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.data.db.entity.TagEntity
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.data.model.JournalPhotoDao
import com.lankeshwar.vaultnote.data.model.JournalPhotoEntity
import java.util.UUID

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class, NoteDraftEntity::class, NoteAttachmentEntity::class, TaskEntity::class,
        JournalEntity::class, JournalPhotoEntity::class],
    version = 19,
    exportSchema = false
)
abstract class VaultNoteDatabase: RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteDraftDao(): NoteDraftDao
    abstract fun noteAttachmentDao(): NoteAttachmentDao
    abstract fun taskDao(): TaskDao
    abstract fun journalDao(): JournalDao
    abstract fun journalPhotosDao(): JournalPhotoDao

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
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN reminderTime INTEGER DEFAULT 0"       )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN reminderRepeat TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                isCompleted INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                dueDate INTEGER
            )
            """.trimIndent()
                )
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_date
            ON journal_entries(date)
            """.trimIndent()
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE journal_entries ADD COLUMN mood TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS journal_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                journalId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(journalId) REFERENCES journal_entries(id)
                ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_journal_photos_journalId ON journal_photos(journalId)"
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN deletedAt INTEGER DEFAULT NULL"
                )
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. NOTES
                db.execSQL("ALTER TABLE notes ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN lastSyncedAt INTEGER")

                // 2. TASKS
                db.execSQL("ALTER TABLE tasks ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN lastSyncedAt INTEGER")

                // 3. JOURNAL ENTRIES
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN lastSyncedAt INTEGER")

                // 4. TAGS
                db.execSQL("ALTER TABLE tags ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tags ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tags ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE tags ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tags ADD COLUMN lastSyncedAt INTEGER")

                // 5. NOTE ATTACHMENTS
                db.execSQL("ALTER TABLE note_attachments ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE note_attachments ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE note_attachments ADD COLUMN remoteStoragePath TEXT")
                db.execSQL("ALTER TABLE note_attachments ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_attachments ADD COLUMN lastSyncedAt INTEGER")

                // 6. JOURNAL PHOTOS
                db.execSQL("ALTER TABLE journal_photos ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE journal_photos ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE journal_photos ADD COLUMN remoteStoragePath TEXT")
                db.execSQL("ALTER TABLE journal_photos ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE journal_photos ADD COLUMN lastSyncedAt INTEGER")

                // 7. NOTE-TAG RELATIONSHIPS
                db.execSQL("ALTER TABLE note_tag_cross_ref ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE note_tag_cross_ref ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_tag_cross_ref ADD COLUMN cloudOwnerId TEXT")
                db.execSQL("ALTER TABLE note_tag_cross_ref ADD COLUMN isCloudDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_tag_cross_ref ADD COLUMN lastSyncedAt INTEGER")


                // Generate a different UUID for every existing row.
                fun populateCloudIds(
                    tableName: String,
                    keyColumns: List<String>
                ) {
                    val selectedColumns = keyColumns.joinToString(", ")
                    val whereClause = keyColumns.joinToString(" AND ") {
                        "$it = ?"
                    }

                    db.query(
                        "SELECT $selectedColumns FROM $tableName"
                    ).use { cursor ->

                        while (cursor.moveToNext()) {

                            val whereArgs = keyColumns.mapIndexed { index, _ ->
                                cursor.getInt(index)
                            }

                            val updateArgs = mutableListOf<Any>(
                                UUID.randomUUID().toString())
                            updateArgs.addAll(whereArgs)

                            db.execSQL(
                                "UPDATE $tableName SET cloudId = ? WHERE $whereClause",
                                updateArgs.toTypedArray()
                            )
                        }
                    }
                }

                populateCloudIds("notes", listOf("id"))
                populateCloudIds("tasks", listOf("id"))
                populateCloudIds("journal_entries", listOf("id"))
                populateCloudIds("tags", listOf("id"))
                populateCloudIds("note_attachments", listOf("id"))
                populateCloudIds("journal_photos", listOf("id"))

                populateCloudIds(
                    "note_tag_cross_ref",
                    listOf("noteId", "tagId")
                )


                // Create unique cloud ID indexes.
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_notes_cloudId ON notes(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_cloudId ON tasks(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_journal_entries_cloudId ON journal_entries(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_tags_cloudId ON tags(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_note_attachments_cloudId ON note_attachments(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_journal_photos_cloudId ON journal_photos(cloudId)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_note_tag_cross_ref_cloudId ON note_tag_cross_ref(cloudId)"
                )
            }
        }






    }

    suspend fun permanentlyDeleteNoteWithAttachments(noteId:Int){
        withTransaction {
            noteAttachmentDao().deleteAttachmentsForNote(noteId)
            noteDao().permanentlyDeleteNote(noteId)
        }
    }

}