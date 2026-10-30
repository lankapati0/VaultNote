package com.lankeshwar.vaultnote.data

data class Note(
    val id:Int = 0,
    val title: String,
    val preview: String,
    val formattedContent: String = "",
    val date: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false,

    val reminderTime: Long? = null,
    val reminderRepeat:String = "NONE",
    val isReminderEnabled: Boolean = false
)
