package com.lankeshwar.vaultnote.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Insert
    suspend fun insertEntry(entry: JournalEntity)

    @Update
    suspend fun updateEntry(entry: JournalEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntity)

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date:String): JournalEntity?

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id:Int): JournalEntity?

}