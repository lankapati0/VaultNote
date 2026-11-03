package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.db.dao.NoteAttachmentDao
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteAttachmentRepository @Inject constructor(private val noteAttachmentDao: NoteAttachmentDao) {

    suspend fun addAttachment(
        attachment: NoteAttachmentEntity
    ): Long{
        return noteAttachmentDao.insertAttachment(attachment)
    }

    fun getAttachmentForNote(noteId: Int): Flow<List<NoteAttachmentEntity>>{
        return noteAttachmentDao.getAttachmentsForNote(noteId )
    }

    suspend fun getAttachmentsForDeletion(noteId: Int): List<NoteAttachmentEntity> {
        return noteAttachmentDao.getAttachmentsForDeletion(noteId)
    }

    // ToDo: Need to use below functions when everything ready
    suspend fun deleteAttachment(noteId:Int){
        noteAttachmentDao.deleteAttachment(noteId)
    }

    suspend fun deleteAttachmentForNote(noteId:Int){
        noteAttachmentDao.deleteAttachmentsForNote(noteId)
    }

    suspend fun getAllPrivateNoteAttachments(): List<NoteAttachmentEntity>{
        return noteAttachmentDao.getAllPrivateNoteAttachments()
    }
    suspend fun updateAttachmentFilePath(attachmentId: Int, newFilePath: File){
        return noteAttachmentDao.updateAttachmentFilePath(attachmentId, newFilePath.absolutePath)
    }
}