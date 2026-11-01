package com.lankeshwar.vaultnote.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity

@Entity(tableName = "journal_photos",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["journalId"])])
data class JournalPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val journalId: Int,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
