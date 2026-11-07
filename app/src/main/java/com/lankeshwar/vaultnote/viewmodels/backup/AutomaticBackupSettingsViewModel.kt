package com.lankeshwar.vaultnote.viewmodels.backup

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.lankeshwar.vaultnote.Utils.BackupScheduler
import com.lankeshwar.vaultnote.Utils.ScheduledBackupPasswordStore
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.AutomaticBackupFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

data class AutomaticBackupSettingsState(
    val isEnabled:Boolean = false,
    val frequency: AutomaticBackupFrequency = AutomaticBackupFrequency.DAILY,
    val isPasswordConfigured: Boolean = false,
    val lastBackupText: String = "No automatic backup yet",
    val errorMessage: String? = null
)

@HiltViewModel
class AutomaticBackupSettingsViewModel @Inject constructor(@ApplicationContext private val context: Context):
    ViewModel(){

    private val preferences = context.getSharedPreferences("automatic_backup_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AutomaticBackupSettingsState())
    val uiState: MutableStateFlow<AutomaticBackupSettingsState> = _uiState

    init {
        loadSettings()
    }

    private fun loadSettings(){
        val frequencyName = preferences.getString("backup_frequency", AutomaticBackupFrequency.DAILY.name)

        val frequency = runCatching {
            AutomaticBackupFrequency.valueOf(frequencyName ?: AutomaticBackupFrequency.DAILY.name)
        }.getOrDefault(AutomaticBackupFrequency.DAILY)

        val password = ScheduledBackupPasswordStore.load(context)
        val passwordConfigured = password != null
        password?.fill('\u0000')

        _uiState.value = AutomaticBackupSettingsState(
            isEnabled = preferences.getBoolean("backup_enabled",false),
            frequency = frequency,
            isPasswordConfigured = passwordConfigured
        )

        if(_uiState.value.isEnabled && passwordConfigured){
            scheduleBackup(frequency)
        } else {
            BackupScheduler.cancelScheduleBackup(context)
        }
    }

    fun updateEnabled(enabled:Boolean){
        if(enabled && !_uiState.value.isPasswordConfigured){
            _uiState.value = _uiState.value.copy(
                isEnabled = false,
                errorMessage = "Please configure a backup password first."
            )
            return
        }
        preferences.edit {
            putBoolean("backup_enabled",true)
                .apply()
        }
        _uiState.value = _uiState.value.copy(
            isEnabled = enabled,
            errorMessage = null
        )
        if(enabled){
            scheduleBackup(_uiState.value.frequency)
        } else {
            BackupScheduler.cancelScheduleBackup(context)
        }
    }

    fun updateFrequency(frequency: AutomaticBackupFrequency){
        preferences.edit { putString("backup_frequency", frequency.name) }
        _uiState.value = _uiState.value.copy(frequency=frequency)

        if(_uiState.value.isEnabled){
            scheduleBackup(frequency)
        }
    }

    fun savePassword(password: CharArray){
        try {
            ScheduledBackupPasswordStore.save(context,password)
            _uiState.value = _uiState.value.copy(
                isPasswordConfigured = true,
                errorMessage = null
            )
            if(_uiState.value.isEnabled){
                scheduleBackup(_uiState.value.frequency)
            }
        }catch (e: Exception){
            _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Could not save backup password.")
            throw e
        }
    }

    private fun scheduleBackup(frequency: AutomaticBackupFrequency){
        when(frequency){
            AutomaticBackupFrequency.DAILY -> BackupScheduler.scheduleDaily(context)

            AutomaticBackupFrequency.WEEKLY -> BackupScheduler.scheduleDaily(context)

            AutomaticBackupFrequency.MONTHLY -> BackupScheduler.scheduleMonthly(context)
        }
    }
}