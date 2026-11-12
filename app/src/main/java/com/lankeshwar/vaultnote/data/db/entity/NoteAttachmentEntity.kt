package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "note_attachments", indices = [Index(value = ["cloudId"], unique = true)])
data class NoteAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val noteId:Int,
    val fileName: String,
    val fileType: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    // Cloud synchronization fields

    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),

    val cloudOwnerId: String? = null,

    val remoteStoragePath: String? = null,

    @ColumnInfo(defaultValue = "0")
    val isCloudDeleted: Boolean = false,

    val lastSyncedAt: Long? = null
)
