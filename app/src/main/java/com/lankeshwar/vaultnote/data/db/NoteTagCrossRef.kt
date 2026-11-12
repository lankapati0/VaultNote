package com.lankeshwar.vaultnote.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.util.UUID

@Entity(tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId","tagId"],
    indices = [Index(value = ["cloudId"], unique = true)])
data class NoteTagCrossRef(
    val noteId:Int,
    val tagId:Int,
    // Cloud synchronization fields

    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),

    val cloudOwnerId: String? = null,

    @ColumnInfo(defaultValue = "0")
    val isCloudDeleted: Boolean = false,

    val lastSyncedAt: Long? = null
)
