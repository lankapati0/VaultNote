package com.lankeshwar.vaultnote.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.driver.SupportSQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [NoteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class VaultNoteDatabase: RoomDatabase() {
    abstract fun noteDao(): NoteDao

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

    }
}