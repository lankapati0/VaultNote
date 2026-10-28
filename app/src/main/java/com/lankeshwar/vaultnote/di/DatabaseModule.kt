package com.lankeshwar.vaultnote.di

import android.content.Context
import androidx.room.Room
import com.lankeshwar.vaultnote.data.NotesRepository
import com.lankeshwar.vaultnote.data.db.NoteDao
import com.lankeshwar.vaultnote.data.db.NoteDraftDao
import com.lankeshwar.vaultnote.data.db.TagDao
import com.lankeshwar.vaultnote.data.db.VaultNoteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VaultNoteDatabase{
        return Room.databaseBuilder(
            context,
            VaultNoteDatabase::class.java,
            "vaultnote_database"
        )
            .addMigrations(VaultNoteDatabase.MIGRATION_1_3, VaultNoteDatabase.MIGRATION_2_3,VaultNoteDatabase.MIGRATION_3_4,VaultNoteDatabase.MIGRATION_4_5,
                VaultNoteDatabase.MIGRATION_5_6,VaultNoteDatabase.MIGRATION_6_7,
                VaultNoteDatabase.MIGRATION_7_8, VaultNoteDatabase.MIGRATION_8_9,VaultNoteDatabase.MIGRATION_9_10,
                VaultNoteDatabase.MIGRATION_10_11)
            .build()
    }

    @Singleton
    @Provides
    fun provideNoteDao(
        database: VaultNoteDatabase
    ): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideTagDao(database: VaultNoteDatabase): TagDao {
        return database.tagDao()
    }

    fun provideNotesRepository(noteDao: NoteDao): NotesRepository{
        return NotesRepository(noteDao)
    }

    @Provides
    @Singleton
    fun provideNoteDraftDao(database: VaultNoteDatabase): NoteDraftDao{
        return database.noteDraftDao()
    }

}