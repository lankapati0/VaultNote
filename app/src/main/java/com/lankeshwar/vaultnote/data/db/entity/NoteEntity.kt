package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.util.UUID

@Entity(tableName = "notes", indices = [Index(value = ["cloudId"], unique = true)])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int=0,
    val title:String,
    val preview: String,
    @ColumnInfo(defaultValue = "")
    val formattedContent: String = "",
    val date: String,

    @ColumnInfo(defaultValue = "0")
    val isPinned:Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isPrivate:Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val reminderTime: Long? = null,
    @ColumnInfo(defaultValue = "NONE")
    val reminderRepeat:String = "NONE",
    @ColumnInfo(defaultValue = "0")
    val isReminderEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    val cloudOwnerId:String? = null,
    val lastSyncedAt:Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isCloudDeleted: Boolean = false
)
