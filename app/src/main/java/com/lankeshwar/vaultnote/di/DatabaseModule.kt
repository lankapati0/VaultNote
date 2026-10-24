package com.lankeshwar.vaultnote.di

import android.content.Context
import androidx.room.Room
import com.lankeshwar.vaultnote.data.db.NoteDao
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
            .addMigrations(VaultNoteDatabase.MIGRATION_1_3, VaultNoteDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideNoteDao(
        database: VaultNoteDatabase
    ): NoteDao {
        return database.noteDao()
    }
}