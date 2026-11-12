package com.lankeshwar.vaultnote.Utils

import android.content.Context
import androidx.core.content.edit

object CloudSyncPreferences {
    private const val PREFS_NAME = "cloud_sync_preferences"
    private const val LAST_DOWNLOAD_KEY = "last_successful_download_at"

    fun getLastDownloadTime(context: android.content.Context): Long{
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return preferences.getLong(LAST_DOWNLOAD_KEY,0L)
    }

    fun saveLastDownloadTime(context: Context,time:Long){
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit { putLong(LAST_DOWNLOAD_KEY, time) }
    }

}