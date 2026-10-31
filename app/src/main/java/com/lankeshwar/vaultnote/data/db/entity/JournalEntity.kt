package com.lankeshwar.vaultnote.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int = 0,
    val date:String,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt:Long = System.currentTimeMillis()
)
