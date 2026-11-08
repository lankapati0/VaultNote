package com.lankeshwar.vaultnote.Utils.backup_trash

import android.content.Context
import android.icu.util.TimeUnit
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lankeshwar.vaultnote.data.db.VaultNoteDatabase
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.repository.NoteAttachmentRepository
import com.lankeshwar.vaultnote.data.storage.AttachmentFileManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteDao: NoteDao,
    private val attachmentRepository: NoteAttachmentRepository,
    private val vaultNoteDatabase: VaultNoteDatabase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {

                val preferences = applicationContext.getSharedPreferences(
                    "trash_retention_settings",
                    Context.MODE_PRIVATE
                )

                val savedDays = preferences.getInt("retention_days", 30)

                // Zero means never automatically delete.
                if (savedDays == 0) {
                    return@withContext Result.success()
                }

                val retentionDays = if (savedDays in listOf(7, 15, 30, 60)) {
                    savedDays
                } else {
                    30
                }

                val cutoffTime = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(retentionDays.toLong())

                val expiredNotes =
                    noteDao.getExpiredDeletedNotes(cutoffTime)

                val attachmentFileManager =
                    AttachmentFileManager(applicationContext)

                for (note in expiredNotes) {

                    val attachments =
                        attachmentRepository.getAttachmentsForDeletion(note.id)

                    var allFilesDeleted = true

                    // Delete physical attachment files first.
                    for (attachment in attachments) {
                        val deleted = attachmentFileManager.deleteAttachment(
                            attachment.filePath
                        )

                        if (!deleted) {
                            allFilesDeleted = false

                            break
                        }
                    }

                    // Keep the note in trash if any file could not be deleted.
                    if (!allFilesDeleted) continue

                    // Delete attachment and note records from Room.
                    vaultNoteDatabase.permanentlyDeleteNoteWithAttachments(note.id)
                }

                Result.success()
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "VaultNoteTrash",
                "Automatic trash cleanup failed",
                e
            )

            Result.retry()
        }
    }
}