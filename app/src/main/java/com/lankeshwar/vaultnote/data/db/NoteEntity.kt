package com.lankeshwar.vaultnote.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
@Entity(tableName = "notes")
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
    val isDeleted: Boolean = false
)
