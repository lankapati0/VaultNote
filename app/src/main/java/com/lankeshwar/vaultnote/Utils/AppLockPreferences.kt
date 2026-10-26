package com.lankeshwar.vaultnote.Utils

import android.content.Context
import androidx.core.content.edit

object AppLockPreferences {

    private const val PREFS_NAME = "vaultnote_app_lock"
    private const val APP_LOCK_ENABLED = "app_lock_enabled"

    fun isAppLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(APP_LOCK_ENABLED,true)
    }

    fun setAppLockEnabled(context: Context,enabled:Boolean){
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(APP_LOCK_ENABLED, enabled) }
    }

}