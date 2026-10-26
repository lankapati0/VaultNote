package com.lankeshwar.vaultnote.Utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

object BiometricPreferences {
    private const val PREFS_NAME = "vaultnote_biometric_settings"
    private const val FINGERPRINT_ENABLED = "fingerprint_enabled"

    fun isFingerprintEnabled(context: Context): Boolean{
        return context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        ).getBoolean(FINGERPRINT_ENABLED,true)
    }

    @SuppressLint("CommitPrefEdits")
    fun setFingerprintEnabled(context: Context, enabled:Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(FINGERPRINT_ENABLED, enabled)
        }
    }

}