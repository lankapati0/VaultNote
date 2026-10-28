package com.lankeshwar.vaultnote.data

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.lankeshwar.vaultnote.data.db.NoteDao
import com.lankeshwar.vaultnote.data.db.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotesRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getWelcomeMessage():String{
        return "Your notes are safely organized."
    }

    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    suspend fun insertNote(note: NoteEntity): Long{
        return noteDao.insertNote(note)
    }

    suspend fun deleteNote(noteId: Int){
        noteDao.deleteNoteById(noteId)
    }

    suspend fun updateNote(note: NoteEntity){
        noteDao.updateNote(note)
    }

    fun getRegularNotes(): Flow<List<NoteEntity>> {
        return noteDao.getRegularNotes()
    }

    fun getPrivateNotes(): Flow<List<NoteEntity>> {
        return noteDao.getPrivateNotes()
    }

    suspend fun updatePinnedStatus(noteId: Int,isPinned: Boolean){
        noteDao.updatePinnedStatus(noteId,isPinned)
    }

    suspend fun updateFavoriteStatus(noteId: Int,isFavorite: Boolean){
        noteDao.updateFavoriteStatus(noteId,isFavorite)
    }

    fun getRecentlyDeletedNotes(): Flow<List<NoteEntity>> =
        noteDao.getRecentlyDeletedNotes()

    suspend fun moveToRecentlyDeleted(noteId: Int) {
        noteDao.moveToRecentlyDeleted(noteId)
    }

    suspend fun restoreNote(noteId: Int) {
        noteDao.restoreNote(noteId)
    }

    suspend fun permanentlyDeleteNote(noteId: Int) {
        noteDao.permanentlyDeleteNote(noteId)
    }



}