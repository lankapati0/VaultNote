package com.lankeshwar.vaultnote.Utils.backup_trash

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object TrashCleanupScheduler{
    private const val UNIQUE_TRASH_CLEANUP_WORK =
        "vaultnote_trash_cleanup"

    fun schedule(context: Context){
        val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(1, TimeUnit.DAYS).build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_TRASH_CLEANUP_WORK,
                ExistingPeriodicWorkPolicy.REPLACE,
                cleanupRequest)
    }


    fun cancel(context:Context){
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_TRASH_CLEANUP_WORK)
    }

}