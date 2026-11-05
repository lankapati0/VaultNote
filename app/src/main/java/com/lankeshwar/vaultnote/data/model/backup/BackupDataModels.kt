package com.lankeshwar.vaultnote.data.model.backup

import kotlinx.serialization.Serializable

@Serializable
data class VaultNoteBackup(
    val formatVersion: Int = 1,
    val appName: String = "VaultNote",
    val createdAt: Long = System.currentTimeMillis(),

    val notes: List<BackupNote> = emptyList(),
    val attachments: List<BackupAttachment> = emptyList(),

    val tags: List<BackupTag> = emptyList(),
    val noteTagCrossRefs: List<BackupNoteTagCrossRef> = emptyList(),

    val drafts: List<BackupNoteDraft> = emptyList(),

    val tasks: List<BackupTask> = emptyList(),
    val journalEntries: List<BackupJournalEntry> = emptyList(),
    val journalPhotos: List<BackupJournalPhoto> = emptyList()
)
@Serializable
data class BackupNote(
    val id:Int,
    val title:String,
    val preview: String,
    val formattedContent:String,
    val date:String,
    val isPinned: Boolean,
    val isFavorite:Boolean,
    val isPrivate:Boolean,
    val isDeleted: Boolean,
    val reminderTime:Long?,
    val reminderRepeat: String,
    val isReminderEnabled: Boolean
)

@Serializable
data class BackupAttachment(
    val id: Int,
    val noteId: Int,
    val fileName: String,
    val fileType: String,
    val fileEntryName: String,
    val createdAt: Long
)
@Serializable
data class BackupTag(
    val id:Int,
    val name:String,
    val colorHex: String
)
@Serializable
data class BackupNoteTagCrossRef(
    val noteId:Int,
    val tagId:Int
)
@Serializable
data class BackupNoteDraft(
    val draftId: String,
    val title: String,
    val content: String,
    val formattedContent: String,
    val isMarkdown: Boolean,
    val updatedAt: Long
)

@Serializable
data class BackupTask(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val dueDate: Long?
)
@Serializable
data class BackupJournalEntry(
    val id: Int,
    val date: String,
    val title: String,
    val content: String,
    val mood: String,
    val createdAt: Long,
    val updatedAt: Long
)
@Serializable
data class BackupJournalPhoto(
    val id: Int,
    val journalId: Int,
    val fileEntryName: String,
    val createdAt: Long
)