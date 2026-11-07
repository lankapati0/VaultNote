package com.lankeshwar.vaultnote.Utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val UNIQUE_BACKUP_WORK =
        "vaultnote_scheduled_backup"

    /**
     * Schedule automatic backups every 24 hours.
     */
    fun scheduleDaily(context: Context){
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_BACKUP_WORK, ExistingPeriodicWorkPolicy.REPLACE,backupRequest)
    }

    /**
     * Schedule automatic backups every 7 days.
     */
    fun scheduleWeekly(context: Context){
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            7, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_BACKUP_WORK, ExistingPeriodicWorkPolicy.REPLACE,backupRequest)
    }

    /**
     * Schedule automatic backups every month.
     */
    fun scheduleMonthly(context: Context){
        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            30, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_BACKUP_WORK, ExistingPeriodicWorkPolicy.REPLACE,backupRequest)
    }

    /**
     * Cancel scheduled automatic backups.
     */
    fun cancelScheduleBackup(context: Context){

        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_BACKUP_WORK)
    }


}
