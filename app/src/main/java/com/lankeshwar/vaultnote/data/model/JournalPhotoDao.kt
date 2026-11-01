package com.lankeshwar.vaultnote.data.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalPhotoDao {
    @Insert
    suspend fun insertPhotos(photo: JournalPhotoEntity)
    @Delete
    suspend fun deletePhotos(photo: JournalPhotoEntity)
    @Query("SELECT * FROM journal_photos WHERE journalId = :journalId ORDER BY createdAt ASC")
    fun getPhotosByJournalId(journalId: Int): Flow<List<JournalPhotoEntity>>
}