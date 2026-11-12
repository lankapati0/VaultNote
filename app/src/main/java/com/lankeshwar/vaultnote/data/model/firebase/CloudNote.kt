package com.lankeshwar.vaultnote.data.model.firebase

data class CloudNote(
    val cloudId: String = "",
    val title: String = "",
    val preview: String = "",
    val formattedContent: String = "",
    val date: String = "",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val reminderTime: Long? = null,
    val reminderRepeat: String = "NONE",
    val isReminderEnabled: Boolean = false,
    val updatedAt: Long = 0L,
    val cloudOwnerId: String = "",
    val isCloudDeleted: Boolean = false,
    val lastSyncedAt: Long? = null
)
