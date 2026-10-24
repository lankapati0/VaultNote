package com.lankeshwar.vaultnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE isPrivate = 0 ORDER BY id DESC")
    fun getRegularNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPrivate = 1 ORDER BY id DESC")
    fun getPrivateNotes(): Flow<List<NoteEntity>>
}