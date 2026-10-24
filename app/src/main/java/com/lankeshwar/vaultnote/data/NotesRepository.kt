package com.lankeshwar.vaultnote.data

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

    suspend fun insertNote(note: NoteEntity){
        noteDao.insertNote(note)
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

}