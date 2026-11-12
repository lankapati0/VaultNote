package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.Utils.PrivateNoteEncryptionManager
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class NotesRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val privateNoteSession: PrivateNoteSession
) {
    fun getWelcomeMessage():String{
        return "Your notes are safely organized."
    }

    suspend fun insertNote(note: NoteEntity): Long{
        val noteToSave = encryptPrivateNote(note)
        return noteDao.insertNote(noteToSave)
    }

    suspend fun deleteNote(noteId: Int){
        noteDao.deleteNoteById(noteId)
    }

    suspend fun migrateExistingPrivateNote(){
        val privateNotes = noteDao.getAllPrivateNotesForMigration()

        val existingEncryptedValue = privateNotes.asSequence()
            .flatMap { note ->
                sequenceOf(note.title,note.preview,note.formattedContent)
            }.firstOrNull{ PrivateNoteEncryptionManager.isEncrypted(it) }

        if(existingEncryptedValue != null){
            PrivateNoteEncryptionManager.decrypt(existingEncryptedValue)
        }

        privateNotes.forEach { note ->
            val encryptedNote = encryptPrivateNote(note)
            if(encryptedNote != note){
                noteDao.updateNote(encryptedNote)
            }
        }
    }

    suspend fun updateNote(note: NoteEntity){
        val noteToSave = encryptPrivateNote(note)
        noteDao.updateNote(noteToSave)
    }

    suspend fun getNoteById(noteId: Int): NoteEntity? {
        return noteDao.getNoteById(noteId)
    }

    fun getRegularNotes(): Flow<List<NoteEntity>> {
        return noteDao.getRegularNotes()
    }

    suspend fun isPrivateNote(noteId: Int): Boolean?{
        return noteDao.isPrivateNote(noteId)
    }
    fun getPrivateNotes(): Flow<List<NoteEntity>> {
        return combine(
            noteDao.getPrivateNotes(),privateNoteSession.isUnlocked
        ){entities,isUnlocked ->
            if(!isUnlocked){
                emptyList()
            } else {
                entities.map { note ->
                    note.copy(
                        title = if(PrivateNoteEncryptionManager.isEncrypted(note.title)){
                            PrivateNoteEncryptionManager.decrypt(note.title)
                        } else {
                            note.title
                        },
                        preview = if(PrivateNoteEncryptionManager.isEncrypted(note.preview)){
                            PrivateNoteEncryptionManager.decrypt(note.preview)
                        } else {
                            note.preview
                        },
                        formattedContent = if(PrivateNoteEncryptionManager.isEncrypted(note.formattedContent)){
                            PrivateNoteEncryptionManager.decrypt(note.formattedContent)
                        } else {
                            note.formattedContent
                        }
                    )
                }
            }
        }
    }

    suspend fun updatePinnedStatus(noteId: Int,isPinned: Boolean){
        noteDao.updatePinnedStatus(noteId,isPinned)
    }

    suspend fun updateFavoriteStatus(noteId: Int,isFavorite: Boolean){
        noteDao.updateFavoriteStatus(noteId,isFavorite)
    }

    fun getRecentlyDeletedNotes(): Flow<List<NoteEntity>> =
        combine(noteDao.getRecentlyDeletedNotes(), privateNoteSession.isUnlocked){entities,isUnlocked ->
            entities.filter { note -> !note.isPrivate || isUnlocked }
                .map { note ->
                    if(!note.isPrivate){
                        note
                    } else {
                        note.copy(
                            title = if(PrivateNoteEncryptionManager.isEncrypted(note.title)){
                                PrivateNoteEncryptionManager.decrypt(note.title)
                            } else {
                                note.title
                            },
                            preview = if(PrivateNoteEncryptionManager.isEncrypted(note.preview)){
                                PrivateNoteEncryptionManager.decrypt(note.preview)
                            } else {
                                note.preview
                            },
                            formattedContent = if(PrivateNoteEncryptionManager.isEncrypted(note.formattedContent)){
                                PrivateNoteEncryptionManager.decrypt(note.formattedContent)
                            } else {
                                note.formattedContent
                            }
                        )
                    }
                }
        }

    suspend fun moveToRecentlyDeleted(noteId: Int) {
        noteDao.moveToRecentlyDeleted(noteId)
    }

    suspend fun restoreNote(noteId: Int) {
        noteDao.restoreNote(noteId)
    }

    suspend fun permanentlyDeleteNote(noteId: Int) {
        noteDao.permanentlyDeleteNote(noteId)
    }

    suspend fun markNoteAsCloudDeleted(noteId:Int){
        noteDao.markNoteAsCloudDeleted(noteId)
    }

    suspend fun updateReminder(noteId:Int, reminderTime:Long?, reminderRepeat:String, isEnabled:Boolean){
        noteDao.updateReminder(noteId,reminderTime,reminderRepeat,isEnabled)
    }

    suspend fun disableOneTimeReminder(noteId:Int){
        noteDao.disableOneTimeReminder(noteId)
    }



    private fun encryptPrivateNote(note: NoteEntity): NoteEntity{
        if(!note.isPrivate) return note

        return note.copy(
            title = if (PrivateNoteEncryptionManager.isEncrypted(note.title)){
                note.title
            } else {
                PrivateNoteEncryptionManager.encrypt(note.title)
            },
            preview = if (PrivateNoteEncryptionManager.isEncrypted(note.preview)) {
                note.preview
            } else {
                PrivateNoteEncryptionManager.encrypt(note.preview)
            },
            formattedContent = if (PrivateNoteEncryptionManager.isEncrypted(note.formattedContent)) {
                note.formattedContent
            } else {
                PrivateNoteEncryptionManager.encrypt(note.formattedContent)
            },
        )
    }
}