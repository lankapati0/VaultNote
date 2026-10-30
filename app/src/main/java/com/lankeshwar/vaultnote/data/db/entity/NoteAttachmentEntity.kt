package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_attachments")
data class NoteAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val noteId:Int,
    val fileName: String,
    val fileType: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
