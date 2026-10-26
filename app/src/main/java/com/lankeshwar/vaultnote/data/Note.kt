package com.lankeshwar.vaultnote.data

data class Note(
    val id:Int = 0,
    val title: String,
    val preview: String,
    val date: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isPrivate: Boolean = false
)
