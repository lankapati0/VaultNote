package com.lankeshwar.vaultnote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: NoteAttachmentEntity): Long

    @Query(
        "SELECT * FROM note_attachments " +
                "WHERE noteId = :noteId ORDER BY createdAt ASC"
    )
    fun getAttachmentsForNote(noteId:Int): Flow<List<NoteAttachmentEntity>>

    @Query("SELECT * FROM note_attachments WHERE noteId = :noteId")
    suspend fun getAttachmentsForDeletion(noteId:Int): List<NoteAttachmentEntity>

    @Query("DELETE FROM note_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId:Int)

    @Query("DELETE FROM note_attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId:Int)

    @Query("""SELECT a.* FROM note_attachments a INNER JOIN notes n ON a.noteId = n.id WHERE n.isPrivate = 1""")
    suspend fun getAllPrivateNoteAttachments(): List<NoteAttachmentEntity>

    @Query("""UPDATE note_attachments SET filePath = :newFilePath WHERE id = :attachmentId""")
    suspend fun updateAttachmentFilePath(attachmentId:Int, newFilePath: String)
}