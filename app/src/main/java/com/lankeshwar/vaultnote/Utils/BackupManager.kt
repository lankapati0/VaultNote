package com.lankeshwar.vaultnote.Utils

import android.content.Context
import com.lankeshwar.vaultnote.data.model.backup.BackupArchiveManager
import com.lankeshwar.vaultnote.data.model.backup.BackupJsonManager
import com.lankeshwar.vaultnote.data.repository.backup.BackupDataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val backupDataRepository: BackupDataRepository,
    @ApplicationContext private val context: Context
){
    suspend fun createBackup(destination: File,password: CharArray) = withContext(Dispatchers.IO){
        val backupData = backupDataRepository.createBackupData()

        try {
            val manifestJson = BackupJsonManager.encodeBackup(backupData.backup)

            BackupArchiveManager.createEncryptedBackup(
                context = context,
                manifestJson = manifestJson,
                files = backupData.files,
                destination = destination,
                password = password
            )
        } finally{
            backupData.cleanup()
            password.fill('\u0000')
        }

    }
}