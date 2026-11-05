package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.Utils.PrivateAttachmentEncryptionManager
import com.lankeshwar.vaultnote.Utils.PrivateNoteEncryptionManager
import com.lankeshwar.vaultnote.data.db.VaultNoteDatabase
import com.lankeshwar.vaultnote.data.model.backup.BackupAttachment
import com.lankeshwar.vaultnote.data.model.backup.BackupJournalEntry
import com.lankeshwar.vaultnote.data.model.backup.BackupJournalPhoto
import com.lankeshwar.vaultnote.data.model.backup.BackupNote
import com.lankeshwar.vaultnote.data.model.backup.BackupNoteDraft
import com.lankeshwar.vaultnote.data.model.backup.BackupNoteTagCrossRef
import com.lankeshwar.vaultnote.data.model.backup.BackupTag
import com.lankeshwar.vaultnote.data.model.backup.BackupTask
import com.lankeshwar.vaultnote.data.model.backup.VaultNoteBackup
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import kotlinx.coroutines.flow.first
import javax.inject.Singleton


import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class BackupDataRepository @Inject constructor(
    private val database: VaultNoteDatabase,
    private val privateNoteSession: PrivateNoteSession,
    @ApplicationContext private val context: Context
) {

    suspend fun createBackupData(): BackupDataResult {

        // Load notes first because we need to know whether private
        // note access is required.
        val notes = database.noteDao().getAllNotes().first()

        // Keep the private-note session unlocked until the entire
        // backup archive has been created.
        val sessionLease = if (notes.any { it.isPrivate }) {
            privateNoteSession.acquireBackupAccess()
                ?: throw IllegalStateException(
                    "Unlock private notes before creating a backup."
                )

        } else {
            null
        }

        val backupAttachments = mutableListOf<BackupAttachment>()
        val backupJournalPhotos = mutableListOf<BackupJournalPhoto>()
        val archiveFiles = mutableMapOf<String, File>()

        // Temporary plaintext files created by decrypting private
        // attachments. These must remain until archive creation ends.
        val temporaryFiles = mutableListOf<File>()

        try {

            // LOAD REMAINING DATABASE DATA

            val tags = database.tagDao().getAllTags().first()

            val crossRefs =
                database.tagDao().getAllNoteTagCrossRefs()

            val drafts =
                database.noteDraftDao().getAllDrafts()

            val tasks =
                database.taskDao().getAllTasks().first()

            val journalEntries =
                database.journalDao().getAllEntries().first()


            // PRIVATE NOTES

            val backupNotes = notes.map { note ->

                if (note.isPrivate &&
                    !privateNoteSession.isUnlocked.value
                ) {
                    throw IllegalStateException(
                        "Private-note session was locked during backup."
                    )
                }

                fun decryptPrivateField(value: String): String {

                    require(
                        PrivateNoteEncryptionManager.isEncrypted(value)
                    ) {
                        "A private note contains data that is not encrypted. " +
                                "Backup cancelled."
                    }

                    return PrivateNoteEncryptionManager.decrypt(value)
                }

                BackupNote(
                    id = note.id,

                    title = if (note.isPrivate) {
                        decryptPrivateField(note.title)
                    } else {
                        note.title
                    },

                    preview = if (note.isPrivate) {
                        decryptPrivateField(note.preview)
                    } else {
                        note.preview
                    },

                    formattedContent = if (note.isPrivate) {
                        decryptPrivateField(note.formattedContent)
                    } else {
                        note.formattedContent
                    },

                    date = note.date,
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    isPrivate = note.isPrivate,
                    isDeleted = note.isDeleted,
                    reminderTime = note.reminderTime,
                    reminderRepeat = note.reminderRepeat,
                    isReminderEnabled = note.isReminderEnabled
                )
            }


            // NOTE ATTACHMENTS

            for (note in notes) {

                val noteAttachments =
                    database.noteAttachmentDao()
                        .getAttachmentsForDeletion(note.id)

                for (attachment in noteAttachments) {

                    val sourceFile = File(attachment.filePath)

                    // Skip missing attachment files.
                    if (!sourceFile.exists() || !sourceFile.isFile) {
                        continue
                    }

                    val entryName =
                        "attachments/${attachment.id}_${sourceFile.name}"

                    val backupFile = if (note.isPrivate) {

                        // Private attachments must be encrypted
                        // on the device before entering the backup.
                        require(
                            PrivateAttachmentEncryptionManager.isEncrypted(
                                sourceFile
                            )
                        ) {
                            "A private attachment is not encrypted: " +
                                    "${sourceFile.name}. Backup cancelled."
                        }

                        val decryptedFile = File(
                            context.cacheDir,
                            "backup_${UUID.randomUUID()}_${sourceFile.name}"
                        )

                        // Register immediately so failure during
                        // decryption still triggers cleanup.
                        temporaryFiles.add(decryptedFile)

                        PrivateAttachmentEncryptionManager.decryptFile(
                            sourceFile,
                            decryptedFile
                        )

                        require(
                            decryptedFile.exists() &&
                                    decryptedFile.isFile
                        ) {
                            "Unable to decrypt private attachment: " +
                                    sourceFile.name
                        }

                        decryptedFile

                    } else {
                        // Public attachments are copied into the
                        // password-protected backup archive.
                        sourceFile
                    }

                    archiveFiles[entryName] = backupFile

                    backupAttachments.add(
                        BackupAttachment(
                            id = attachment.id,
                            noteId = attachment.noteId,
                            fileName = attachment.fileName,
                            fileType = attachment.fileType,
                            fileEntryName = entryName,
                            createdAt = attachment.createdAt
                        )
                    )
                }
            }


            // DAILY JOURNAL PHOTOS

            for (journal in journalEntries) {

                val photos = database.journalPhotosDao()
                    .getPhotosByJournalId(journal.id)
                    .first()

                for (photo in photos) {

                    val sourceFile = File(photo.filePath)

                    if (!sourceFile.exists() || !sourceFile.isFile) {
                        continue
                    }

                    val entryName =
                        "journal_photos/${photo.id}_${sourceFile.name}"

                    archiveFiles[entryName] = sourceFile

                    backupJournalPhotos.add(
                        BackupJournalPhoto(
                            id = photo.id,
                            journalId = photo.journalId,
                            fileEntryName = entryName,
                            createdAt = photo.createdAt
                        )
                    )
                }
            }


            // BUILD BACKUP DATA

            val backup = VaultNoteBackup(

                notes = backupNotes,

                attachments = backupAttachments,

                tags = tags.map {
                    BackupTag(
                        id = it.id,
                        name = it.name,
                        colorHex = it.colorHex
                    )
                },

                noteTagCrossRefs = crossRefs.map {
                    BackupNoteTagCrossRef(
                        noteId = it.noteId,
                        tagId = it.tagId
                    )
                },

                drafts = drafts.map {
                    BackupNoteDraft(
                        draftId = it.draftId,
                        title = it.title,
                        content = it.content,
                        formattedContent = it.formattedContent,
                        isMarkdown = it.isMarkdown,
                        updatedAt = it.updatedAt
                    )
                },

                tasks = tasks.map {
                    BackupTask(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        isCompleted = it.isCompleted,
                        createdAt = it.createdAt,
                        dueDate = it.dueDate
                    )
                },

                journalEntries = journalEntries.map {
                    BackupJournalEntry(
                        id = it.id,
                        date = it.date,
                        title = it.title,
                        content = it.content,
                        mood = it.mood,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                },

                journalPhotos = backupJournalPhotos
            )

            // Ownership of temporary files and session lease
            // transfers to BackupDataResult after successful collection.
            return BackupDataResult(
                backup = backup,
                files = archiveFiles,
                temporaryFiles = temporaryFiles,
                sessionLease = sessionLease
            )

        } catch (exception: Throwable) {

            // Collection failed. Clean up any decrypted files.
            temporaryFiles.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }

            // Release the session lease because no result was returned.
            sessionLease?.close()
            android.util.Log.d(
                "VaultNoteBackup",
                "Backup repository unlocked: ${privateNoteSession.isUnlocked.value}"
            )
            throw exception
        }
    }
}


class BackupDataResult(
    val backup: VaultNoteBackup,
    val files: Map<String, File>,
    private val temporaryFiles: List<File>,
    private val sessionLease: AutoCloseable?
) {

    private var cleanedUp = false

    @Synchronized
    fun cleanup() {

        if (cleanedUp) return

        cleanedUp = true

        try {
            temporaryFiles.forEach { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
        } finally {
            // Release the private-note session after archive creation.
            sessionLease?.close()
        }
    }
}
