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

    @Query("SELECT * FROM notes WHERE isPrivate = 0 AND isDeleted = 0 ORDER BY id DESC")
    fun getRegularNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPrivate = 1 AND isDeleted = 0 ORDER BY id DESC")
    fun getPrivateNotes(): Flow<List<NoteEntity>>
    @Query("""
    SELECT * FROM notes
    WHERE isPrivate = 0
    AND (title LIKE '%' || :query || '%'
    OR preview LIKE '%' || :query || '%')
    ORDER BY id DESC
""")
    fun searchRegularNotes(query: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun updatePinnedStatus(noteId: Int, isPinned: Boolean)

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :noteId")
    suspend fun updateFavoriteStatus(noteId: Int, isFavorite: Boolean)

    @Query("UPDATE notes SET isDeleted = 1 WHERE id = :noteId")
    suspend fun moveToRecentlyDeleted(noteId:Int)

    @Query("UPDATE notes SET isDeleted = 0 WHERE id = :noteId")
    suspend fun restoreNote(noteId: Int)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun permanentlyDeleteNote(noteId: Int)

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY id DESC")
    fun getRecentlyDeletedNotes(): Flow<List<NoteEntity>>
}