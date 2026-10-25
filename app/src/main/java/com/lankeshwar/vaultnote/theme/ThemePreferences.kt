package com.lankeshwar.vaultnote.theme

import android.content.Context
import androidx.core.content.edit

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemePreferences {
    private const val PREFS_NAME = "vaultnote_settings"
    private const val THEME_KEY = "app_theme"

    fun saveTheme(context: Context, theme: AppTheme){
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(THEME_KEY,theme.name)
        }
    }

    fun getTheme(context: Context): AppTheme{
        val savedTheme = context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
            .getString(THEME_KEY, AppTheme.SYSTEM.name)
        return runCatching {
            AppTheme.valueOf(savedTheme ?: AppTheme.SYSTEM.name)
        }.getOrDefault(AppTheme.SYSTEM)
    }
}