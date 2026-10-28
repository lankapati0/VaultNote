package com.lankeshwar.vaultnote.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM note_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId:Int)

    @Query("DELETE FROM note_attachments WHERE noteId = :noteId")
    suspend fun deleteAttachmentsForNote(noteId:Int)

}