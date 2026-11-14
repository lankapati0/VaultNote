package com.lankeshwar.vaultnote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE isPrivate = 0 AND isDeleted = 0 ORDER BY id DESC")
    fun getRegularNotes(): Flow<List<NoteEntity>>

    @Query(""" SELECT * FROM notes WHERE isPrivate = :isPrivate AND isDeleted = 0 ORDER BY id DESC""")
    fun getNotesForRelatedNotes(isPrivate:Boolean): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPrivate = 1 AND isDeleted = 0 ORDER BY id DESC")
    fun getPrivateNotes(): Flow<List<NoteEntity>>

    @Query("SELECT isPrivate FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun isPrivateNote(noteId:Int): Boolean?

    @Query("SELECT * FROM notes WHERE isPrivate = 1 ORDER BY id DESC")
    suspend fun getAllPrivateNotesForMigration(): List<NoteEntity>

    @Query("""SELECT * FROM notes WHERE isPrivate = 0 AND (title LIKE '%' || :query || '%' OR preview LIKE '%' || :query || '%')
    ORDER BY id DESC""")
    fun searchRegularNotes(query: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun updatePinnedStatus(noteId: Int, isPinned: Boolean)

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE id = :noteId")
    suspend fun updateFavoriteStatus(noteId: Int, isFavorite: Boolean)

    @Query(""" UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :noteId""")
    suspend fun moveToRecentlyDeleted(noteId:Int, deletedAt:Long = System.currentTimeMillis())

    @Query("""UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE id = :noteId""")
    suspend fun restoreNote(noteId: Int)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun permanentlyDeleteNote(noteId: Int)

    @Query(""" UPDATE notes SET isCloudDeleted = 1, isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :noteId""")
    suspend fun markNoteAsCloudDeleted(noteId: Int,deletedAt:Long= System.currentTimeMillis(),updatedAt:Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY id DESC")
    fun getRecentlyDeletedNotes(): Flow<List<NoteEntity>>

    @Query("""UPDATE notes SET reminderTime = :reminderTime, reminderRepeat = :reminderRepeat, isReminderEnabled = :isEnabled WHERE id = :noteId""")
    suspend fun updateReminder(noteId:Int, reminderTime:Long?, reminderRepeat: String, isEnabled: Boolean)

    @Query("""UPDATE notes SET isReminderEnabled = 0 WHERE id = :noteId AND reminderRepeat = 'NONE'""")
    suspend fun disableOneTimeReminder(noteId:Int){

    }

    @Query("""SELECT * FROM notes WHERE isPrivate = 0 AND isDeleted = 0 ORDER BY id DESC LIMIT 3""")
    suspend fun getRecentNotesForWidget(): List<NoteEntity>

    @Query("""SELECT * FROM notes WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt <= :cutoffTime""")
    suspend fun getExpiredDeletedNotes(cutoffTime:Long): List<NoteEntity>

    @Query("""SELECT * FROM notes WHERE isPrivate = 0 AND isCloudDeleted = 0 AND ( lastSyncedAt IS NULL OR updatedAt > lastSyncedAt)""")
    suspend fun getPublicNotesForCloudSync(): List<NoteEntity>

    @Query(""" SELECT * FROM notes WHERE isPrivate = 0 AND isCloudDeleted = 1 AND cloudOwnerId = :userId AND ( lastSyncedAt IS NULL OR updatedAt > lastSyncedAt)""")
    suspend fun getPublicNotesPendingCloudDeletion(userId: String):List<NoteEntity>

    @Query(""" SELECT * FROM notes WHERE cloudId = :cloudId LIMIT 1""")
    suspend fun getNoteByCloudId(cloudId: String): NoteEntity?

    @Query(""" UPDATE notes SET cloudOwnerId = :userId WHERE cloudId = :cloudId AND (cloudOwnerId IS NULL OR cloudOwnerId = :userId)""")
    suspend fun assignNoteCloudOwner(cloudId: String, userId: String): Int

    @Query("""UPDATE notes SET lastSyncedAt = :syncedAt WHERE cloudId IN (:cloudId) AND cloudOwnerId = :userId""")
    suspend fun updateNoteLastSyncedAt(cloudId: List<String>, userId: String, syncedAt: Long): Int

    @Query(""" UPDATE notes SET lastSyncedAt = :syncedAt WHERE cloudId IN (:cloudIds) AND cloudOwnerId = :userId""")
    suspend fun updateNotesLastSyncedAt(cloudIds: String, userId: String,syncedAt: Long): Int

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId:Int): NoteEntity?
}
