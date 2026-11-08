package com.lankeshwar.vaultnote.viewmodels.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import androidx.core.content.edit
import com.lankeshwar.vaultnote.Utils.backup_trash.TrashCleanupScheduler

@HiltViewModel
class TrashRetentionSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
): ViewModel(){

    private val preferences = context.getSharedPreferences(
        "trash_retention_settings",
        Context.MODE_PRIVATE
    )

    private val _retentionDays = MutableStateFlow(
        preferences.getInt("retention_days", 30)
    )

    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()
    init {
        updateCleanupSchedule(_retentionDays.value)
    }
    fun updateRetentionDays(days:Int){

        if(days !in listOf(0,7,15,30,60)) return

        preferences.edit {
            putInt("retention_days", days)
        }

        _retentionDays.value = days

        updateCleanupSchedule(days)
    }

    private fun updateCleanupSchedule(days:Int){
        if(days == 0){
            TrashCleanupScheduler.cancel(context)
        } else {
            TrashCleanupScheduler.schedule(context)
        }
    }
}