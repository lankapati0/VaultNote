package com.lankeshwar.vaultnote.data

import com.lankeshwar.vaultnote.data.db.NoteAttachmentDao
import com.lankeshwar.vaultnote.data.db.NoteAttachmentEntity
import kotlinx.coroutines.flow.Flow
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

    suspend fun deleteAttachment(noteId:Int){
        noteAttachmentDao.deleteAttachment(noteId)
    }

    suspend fun deleteAttachmentForNote(noteId:Int){
        noteAttachmentDao.deleteAttachmentsForNote(noteId)
    }


}