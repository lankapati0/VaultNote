package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["date"], unique = true), Index(value = ["cloudId"], unique = true)]
)
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val date:String,
    val title: String,
    val content: String,
    @ColumnInfo(defaultValue = "''")
    val mood: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt:Long = System.currentTimeMillis(),
    // Cloud synchronization fields
    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),

    val cloudOwnerId: String? = null,

    @ColumnInfo(defaultValue = "0")
    val isCloudDeleted: Boolean = false,

    val lastSyncedAt: Long? = null
)
