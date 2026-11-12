package com.lankeshwar.vaultnote.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lankeshwar.vaultnote.data.repository.NotesRepository
import com.lankeshwar.vaultnote.data.db.dao.NoteAttachmentDao
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.db.dao.NoteDraftDao
import com.lankeshwar.vaultnote.data.db.dao.TagDao
import com.lankeshwar.vaultnote.data.db.VaultNoteDatabase
import com.lankeshwar.vaultnote.data.db.dao.JournalDao
import com.lankeshwar.vaultnote.data.db.dao.TaskDao
import com.lankeshwar.vaultnote.data.model.JournalPhotoDao
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
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
                VaultNoteDatabase.MIGRATION_10_11,
                VaultNoteDatabase.MIGRATION_11_12,VaultNoteDatabase.MIGRATION_12_13,
                VaultNoteDatabase.MIGRATION_13_14,
                VaultNoteDatabase.MIGRATION_14_15,VaultNoteDatabase.MIGRATION_15_16,
                VaultNoteDatabase.MIGRATION_16_17,VaultNoteDatabase.MIGRATION_17_18,
                VaultNoteDatabase.MIGRATION_18_19)
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

    @Provides
    @Singleton
    fun provideNotesRepository(noteDao: NoteDao,privateNoteSession: PrivateNoteSession): NotesRepository{
        return NotesRepository(
            noteDao,
            privateNoteSession
        )
    }

    @Provides
    @Singleton
    fun provideNoteDraftDao(database: VaultNoteDatabase): NoteDraftDao{
        return database.noteDraftDao()
    }

    @Provides
    @Singleton
    fun provideNoteAttachmentDao(database: VaultNoteDatabase): NoteAttachmentDao {
        return database.noteAttachmentDao()
    }

    @Provides
    fun provideTaskDao(database: VaultNoteDatabase): TaskDao{
        return database.taskDao()
    }
    @Provides
    fun provideJournalDao(database: VaultNoteDatabase): JournalDao {
        return database.journalDao()
    }
    @Provides
    fun provideJournalPhotoDao(database: VaultNoteDatabase): JournalPhotoDao {
        return database.journalPhotosDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}