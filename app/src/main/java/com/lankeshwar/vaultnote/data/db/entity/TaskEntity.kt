package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks", indices = [Index(value = ["cloudId"], unique = true)])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val title:String,
    val description:String = "",
    val isCompleted: Boolean = false,
    val createdAt:Long = System.currentTimeMillis(),
    val dueDate:Long? = null,
    @ColumnInfo(defaultValue = "")
    val cloudId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAt:Long= System.currentTimeMillis(),
    val cloudOwnerId: String? = null,

    @ColumnInfo(defaultValue = "0")
    val isCloudDeleted: Boolean = false,

    val lastSyncedAt: Long? = null
)
