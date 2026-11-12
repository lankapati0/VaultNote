package com.lankeshwar.vaultnote.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import java.util.UUID

@Entity(tableName = "journal_photos",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["journalId"]), Index(value = ["cloudId"], unique = true)])
data class JournalPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val journalId: Int,
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
