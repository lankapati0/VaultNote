package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true), Index(value = ["cloudId"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val colorHex: String = "#64B5F6",
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