package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_drafts")
data class NoteDraftEntity(
    @PrimaryKey
    val draftId: String,
    val title: String,
    val content: String,
    val formattedContent: String,
    val isMarkdown: Boolean,
    val updatedAt: Long
)
