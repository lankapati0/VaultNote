package com.lankeshwar.vaultnote.Utils.backup_trash
import androidx.hilt.work.HiltWorker
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lankeshwar.vaultnote.Utils.PinManager
import com.lankeshwar.vaultnote.Utils.ScheduledBackupPasswordStore
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val privateNoteSession: PrivateNoteSession
): CoroutineWorker(appContext, workerParams){

    override suspend fun doWork(): Result {
        var password: CharArray? = null
        var backupFile: File? = null
        var backupLease: AutoCloseable? = null

        try {
            password =
                ScheduledBackupPasswordStore.load(applicationContext) ?: return Result.failure()

            if (PinManager.hasPin(applicationContext)) {
                val lease = privateNoteSession.acquireBackupAccess()
                if(lease == null){
                    return Result.failure()
                }
                backupLease = lease
            }
            return withContext(Dispatchers.IO) {
                val backupDirectory = File(applicationContext.filesDir, "local_backups")
                if (!backupDirectory.exists() && !backupDirectory.mkdirs()) {
                    throw IOException("Could not create backup directory.")
                }

                backupFile =
                    File(backupDirectory, "VaultNote_Auto_\${System.currentTimeMillis()}.vnb")

                backupManager.createBackup(destination = backupFile!!, password = password!!)

                check(backupFile!!.exists() && backupFile!!.length() > 0L) {
                    "Automatic backup creation failed."
                }
                cleanupOldAutomaticBackups()
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("VaultNoteBackup", "Scheduled backup failed", e)
            backupFile?.delete()
            return Result.failure()
        } finally {
            password?.fill('\u0000')
            backupLease?.close()
        }

    }

    private fun cleanupOldAutomaticBackups(){
        val backupDirectory = File(applicationContext.filesDir,"local_backups")
        if(!backupDirectory.exists()) return

        val automaticBackups = backupDirectory.listFiles()?.filter {file ->
            file.isFile && file.extension == "vnb" && file.name.startsWith("VaultNote_Auto_")
        }?.sortedByDescending {it.lastModified()}?: return

        val oldBackups = automaticBackups.drop(MAX_AUTO_BACKUPS)

        oldBackups.forEach{ backup ->
            if (!backup.delete()) {
                Log.w(
                    "VaultNoteBackup",
                    "Could not delete old automatic backup: ${backup.name}"
                )
            }
        }
    }

    companion object {
        private const val MAX_AUTO_BACKUPS = 5

    }
}