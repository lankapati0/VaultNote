package com.lankeshwar.vaultnote.data.repository.backup

import android.content.Context
import androidx.compose.ui.text.input.OffsetMapping
import androidx.room.withTransaction
import com.lankeshwar.vaultnote.Utils.PrivateAttachmentEncryptionManager
import com.lankeshwar.vaultnote.Utils.PrivateNoteEncryptionManager
import com.lankeshwar.vaultnote.data.db.NoteTagCrossRef
import com.lankeshwar.vaultnote.data.db.VaultNoteDatabase
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.data.db.entity.TagEntity
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.data.model.JournalPhotoEntity
import com.lankeshwar.vaultnote.data.model.backup.BackupArchiveManager
import com.lankeshwar.vaultnote.data.model.backup.BackupAttachment
import com.lankeshwar.vaultnote.data.model.backup.BackupJournalEntry
import com.lankeshwar.vaultnote.data.model.backup.BackupJournalPhoto
import com.lankeshwar.vaultnote.data.model.backup.BackupJsonManager
import com.lankeshwar.vaultnote.data.model.backup.BackupNote
import com.lankeshwar.vaultnote.data.model.backup.BackupNoteDraft
import com.lankeshwar.vaultnote.data.model.backup.BackupNoteTagCrossRef
import com.lankeshwar.vaultnote.data.model.backup.BackupTag
import com.lankeshwar.vaultnote.data.model.backup.BackupTask
import com.lankeshwar.vaultnote.data.model.backup.ExtractedBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class BackupPreview(
    val createdAt:Long,
    val noteCount:Int, val privateNoteCount:Int, val attachmentCount:Int, val tagCount:Int, val draftCount:Int, val taskCount:Int,
    val journalEntryCount:Int, val journalPhotoCount:Int

)

class BackupRestoreRepository @Inject constructor(
    private val database: VaultNoteDatabase,
    @ApplicationContext private val context: Context
){

    suspend fun previewBackup(backupFile: File, password: CharArray): BackupPreview = withContext(
        Dispatchers.IO){
            var extractedBackup: ExtractedBackup? = null
        try {
            require(backupFile.exists() && backupFile.isFile) {
                "Backup file does not exist."
            }

            require(backupFile.length() > 0) {
                "Backup file is empty."
            }

            extractedBackup = BackupArchiveManager.extractEncryptedBackup(
                context = context, backupFile = backupFile, password = password
            )
            val backup = BackupJsonManager.decodeBackup(extractedBackup.manifestJson)

            BackupJsonManager.validateBackupFiles(backup = backup, extractedFiles = extractedBackup.extractedFile)

            BackupPreview(
                createdAt = backup.createdAt,
                noteCount = backup.notes.size,
                privateNoteCount = backup.notes.count{it.isPrivate},
                attachmentCount = backup.attachments.size,
                tagCount = backup.tags.size,
                draftCount = backup.drafts.size,
                taskCount = backup.tasks.size,
                journalEntryCount = backup.journalEntries.size,
                journalPhotoCount = backup.journalPhotos.size
            )
        }finally{
            // Remove decrypted archive contents.
            extractedBackup?.cleanup()

            // Clear the password from memory.
            password.fill('\u0000')
        }
    }

    suspend fun restoreBackup(backupFile: File,password: CharArray) = withContext(Dispatchers.IO){
        var extractedBackup: ExtractedBackup? = null
        val createdFiles = mutableListOf<File>()
        try {
            require(backupFile.exists() && backupFile.isFile) {
                "Backup file does not exist."
            }

            require(backupFile.length() > 0) {
                "Backup file is empty."
            }
            extractedBackup = BackupArchiveManager.extractEncryptedBackup(
                context = context, backupFile = backupFile,password = password
            )

            val backup = BackupJsonManager.decodeBackup(extractedBackup.manifestJson)

            BackupJsonManager.validateBackupFiles(backup=backup, extractedFiles = extractedBackup.extractedFile)
            database.withTransaction {
                val noteIdMapping = restoreNotes(backup.notes)

                restoreAttachments(
                    backupAttachments = backup.attachments,
                    backupNotes = backup.notes, extractedFiles = extractedBackup.extractedFile,
                    noteIdMappings = noteIdMapping,
                    createdFiles = createdFiles
                )
                val tagIdMapping = restoreTags(backup.tags)

                restoreTagAssignments(
                    backupCrossRef = backup.noteTagCrossRefs,
                    noteIdMapping = noteIdMapping,
                    tagIdMapping = tagIdMapping
                )
                restoreDrafts(backup.drafts)

                restoreTasks(backup.tasks)
                val (journalIdMapping,skippedJournalIds) = restoreJournalEntries(backup.journalEntries)

                restoreJournalPhotos(backup.journalPhotos,
                    extractedFile = extractedBackup.extractedFile,
                    journalIdMapping = journalIdMapping,
                    skippedJournalIds = skippedJournalIds,
                    createdFiles = createdFiles)
            }
        }catch (e: Throwable){
              createdFiles.forEach { file ->
                  file.delete()
              }
            throw e
        } finally {
            extractedBackup?.cleanup()
            password.fill('\u0000')
        }
    }


    private suspend fun restoreNotes(
        backupNotes: List<BackupNote>
    ): Map<Int, Int> {
        val noteIdMapping = mutableMapOf<Int, Int>()
        val existingNotes = database.noteDao()
            .getAllNotes()
            .first()
            .toMutableList()

        fun readableValue(value: String, isPrivate: Boolean): String {
            return if (
                isPrivate &&
                PrivateNoteEncryptionManager.isEncrypted(value)
            ) {
                PrivateNoteEncryptionManager.decrypt(value)
            } else {
                value
            }
        }

        for (backupNote in backupNotes) {

            // First, match using the original database ID.
            // This works even if the note was edited after backup.
            val existingNote = existingNotes.find {
                it.id == backupNote.id && it.isPrivate == backupNote.isPrivate
            } ?: existingNotes.find { existing ->

                // Fallback for backups restored with different IDs.
                existing.isPrivate == backupNote.isPrivate &&
                        existing.date == backupNote.date &&
                        readableValue(existing.title, existing.isPrivate) ==
                        backupNote.title &&
                        readableValue(
                            existing.formattedContent,
                            existing.isPrivate
                        ) == backupNote.formattedContent
            }

            if (existingNote != null) {
                noteIdMapping[backupNote.id] = existingNote.id
                continue
            }

            val restoredTitle = if (backupNote.isPrivate) {
                PrivateNoteEncryptionManager.encrypt(backupNote.title)
            } else {
                backupNote.title
            }

            val restoredPreview = if (backupNote.isPrivate) {
                PrivateNoteEncryptionManager.encrypt(backupNote.preview)
            } else {
                backupNote.preview
            }

            val restoredContent = if (backupNote.isPrivate) {
                PrivateNoteEncryptionManager.encrypt(
                    backupNote.formattedContent
                )
            } else {
                backupNote.formattedContent
            }

            val newNote = NoteEntity(
                id = 0,
                title = restoredTitle,
                preview = restoredPreview,
                formattedContent = restoredContent,
                date = backupNote.date,
                isPinned = backupNote.isPinned,
                isFavorite = backupNote.isFavorite,
                isPrivate = backupNote.isPrivate,
                isDeleted = backupNote.isDeleted,
                reminderTime = backupNote.reminderTime,
                reminderRepeat = backupNote.reminderRepeat,
                isReminderEnabled = backupNote.isReminderEnabled
            )

            val newId = database.noteDao()
                .insertNote(newNote)
                .toInt()

            noteIdMapping[backupNote.id] = newId
            existingNotes.add(newNote.copy(id = newId))
        }

        return noteIdMapping
    }


    private suspend fun restoreAttachments(
        backupAttachments: List<BackupAttachment>,
        backupNotes: List<BackupNote>,
        extractedFiles: Map<String, File>,
        noteIdMappings: Map<Int,Int>,
        createdFiles: MutableList<File>
    ){
        val attachmentDirectory = File(context.filesDir, "attachments").apply { if(!exists()) mkdirs()}

        val restoreNotesById = database.noteDao().getAllNotes().first().associateBy { it.id }
        val privateNoteIds = backupNotes.mapNotNull { backupNote ->
            val restoreId = noteIdMappings[backupNote.id]

            if(restoreId!=null && restoreNotesById[restoreId]?.isPrivate == true){
                backupNote.id
            } else {
                null
            }
        }.toSet()
        for(attachment in backupAttachments){
            val newNoteId = noteIdMappings[attachment.noteId] ?: throw IllegalStateException("Restored note ID not found for attachment.")

            val existingAttachments = database.noteAttachmentDao().getAttachmentsForDeletion(newNoteId)

            val alreadyExists = existingAttachments.any {
                it.fileName == attachment.fileName &&
                it.fileType == attachment.fileType &&
                it.createdAt == attachment.createdAt
            }

            if(alreadyExists){
                continue
            }

            val sourceFile = extractedFiles[attachment.fileEntryName] ?: throw IllegalStateException( "Attachment file missing from backup: ${attachment.fileName}")

            require(sourceFile.exists() && sourceFile.isFile) {
                "Invalid attachment file: ${attachment.fileName}"
            }
            val isPrivate = attachment.noteId in privateNoteIds
            val extension = attachment.fileName.substringAfterLast('.', "")
                            .filter { it.isLetterOrDigit() }.take(10)

            val destinationName = if(isPrivate){"${UUID.randomUUID().toString()}.enc"} else {
                UUID.randomUUID().toString() + if(extension.isNotBlank()) ".$extension" else ""
            }

            val destinationFile = File(attachmentDirectory,destinationName)

            try {
                if(isPrivate){
                    PrivateAttachmentEncryptionManager.encryptFile(sourceFile,destinationFile)
                } else {
                    sourceFile.copyTo(destinationFile)
                }
                createdFiles.add(destinationFile)

                database.noteAttachmentDao().insertAttachment(
                    NoteAttachmentEntity(
                        id=0,
                        noteId = newNoteId,
                        fileName = attachment.fileName, fileType = attachment.fileType,
                        filePath = destinationFile.absolutePath,
                        createdAt = attachment.createdAt
                    )
                )
            }catch (e: Exception){
                destinationFile.delete()
                throw e
            }

        }
    }

    private suspend fun restoreTags(backupTags: List<BackupTag>): Map<Int,Int> {
        val tagIdMapping = mutableMapOf<Int,Int>()
        for (backupTag in backupTags){
            val insertedId = database.tagDao().insertTag(
                TagEntity(id=0,
                    name = backupTag.name,
                    colorHex = backupTag.colorHex)
            )

            val restoredTagId = if(insertedId != -1L){
                insertedId.toInt()
            } else {
                database.tagDao().getAllTags().first().first{it.name == backupTag.name}.id
            }
            tagIdMapping[backupTag.id] = restoredTagId
        }
        return tagIdMapping
    }

    private suspend fun restoreTagAssignments(
        backupCrossRef: List<BackupNoteTagCrossRef>,
        noteIdMapping: Map<Int,Int>,
        tagIdMapping: Map<Int,Int>
    ){
        for(crossRef in backupCrossRef){
            val newNoteId = noteIdMapping[crossRef.noteId]
                ?: throw IllegalStateException("Restored note ID not found for tag assignment.")

            val newTagId = tagIdMapping[crossRef.tagId] ?: throw IllegalStateException("Restored tag ID not found for note assignment.")

            database.tagDao().addTagToNote(
                NoteTagCrossRef(noteId = newNoteId,
                    tagId = newTagId)
            )

        }
    }

    private suspend fun restoreDrafts(
        backupDrafts: List<BackupNoteDraft>
    ) {
        for (draft in backupDrafts) {

            // Check whether this draft already exists.
            val existingDraft = database.noteDraftDao()
                .getDraft(draft.draftId)

            // Preserve existing drafts.
            // Restore only drafts that don't already exist.
            if (existingDraft == null) {

                database.noteDraftDao().saveDraft(
                    NoteDraftEntity(
                        draftId = draft.draftId,
                        title = draft.title,
                        content = draft.content,
                        formattedContent = draft.formattedContent,
                        isMarkdown = draft.isMarkdown,
                        updatedAt = draft.updatedAt
                    )
                )
            }
        }
    }

    private suspend fun restoreTasks(backupTask: List<BackupTask>){
        val existingTasks = database.taskDao().getAllTasks().first().toMutableList()
        for(task in backupTask){
            val existingTask = existingTasks.find { existing ->
                existing.title == task.title &&
                existing.description == task.description &&
                existing.createdAt == task.createdAt &&
                existing.dueDate == task.dueDate
            }

            if(existingTask != null){
                val updatedTask = existingTask.copy(
                    title = task.title,
                    description = task.description,
                    isCompleted = task.isCompleted,
                    createdAt = task.createdAt,
                    dueDate = task.dueDate
                )

                database.taskDao().updateTask(updatedTask)

                val index = existingTasks.indexOf(existingTask)
                existingTasks[index] = updatedTask
            } else {
                val newTask = TaskEntity(
                    id = 0,
                    createdAt = task.createdAt,
                    dueDate = task.dueDate,
                    description = task.description,
                    title = task.title,
                    isCompleted = task.isCompleted

                )
                database.taskDao().insertTask(newTask)
                existingTasks.add(newTask)
            }
        }
    }

    private suspend fun restoreJournalEntries(backupEntries: List<BackupJournalEntry>): Pair<Map<Int,Int>, Set<Int>>{
        val journalIdMapping = mutableMapOf<Int,Int>()
        val skippedJournalIds = mutableSetOf<Int>()

        for(entry in backupEntries){
            val existingEntry = database.journalDao().getEntryByDate(entry.date)
            if(existingEntry != null){
                skippedJournalIds.add(entry.id)
                continue
            }

            val newJournalId = database.journalDao().insertEntry(
                JournalEntity(
                    id=0, date = entry.date,
                    title = entry.title, content = entry.content,
                    mood = entry.mood, createdAt = entry.createdAt,
                    updatedAt = entry.updatedAt
                )
            )
            journalIdMapping[entry.id] = newJournalId.toInt()
        }
        return Pair(journalIdMapping,skippedJournalIds)
    }

    private suspend fun restoreJournalPhotos(
        backupPhotos: List<BackupJournalPhoto>,
        extractedFile: Map<String, File>,
        journalIdMapping: Map<Int, Int>,
        skippedJournalIds: Set<Int>,
        createdFiles: MutableList<File>
    ) {
        val destinationDirectory = File(
            context.filesDir,
            "journal_photos"
        )

        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs()
        }

        backupPhotos.forEach { backupPhoto ->

            if(backupPhoto.journalId in skippedJournalIds){
                return@forEach
            }

            val newJournalId = journalIdMapping[backupPhoto.journalId]
                ?: throw IllegalStateException("Restored journal ID not found for photo.")

            val sourceFile = extractedFile[backupPhoto.fileEntryName]
                ?: throw IllegalStateException(
                    "Journal photo file is missing: ${backupPhoto.fileEntryName}"
                )
            require(sourceFile.exists() && sourceFile.isFile) {
                "Invalid journal photo file: ${backupPhoto.fileEntryName}"
            }
            val extension = sourceFile.extension.ifBlank { "jpg" }

            val destinationFile = File(
                destinationDirectory,
                "${UUID.randomUUID()}.$extension"
            )

            try {
                sourceFile.copyTo(destinationFile)
                createdFiles.add(destinationFile)
                database.journalPhotosDao().insertPhotos(
                    JournalPhotoEntity(
                        id = 0,
                        journalId = newJournalId,
                        filePath = destinationFile.absolutePath,
                        createdAt = backupPhoto.createdAt
                    )
                )

            } catch (e: Exception) {
                destinationFile.delete()
                throw e
            }
        }
    }
}