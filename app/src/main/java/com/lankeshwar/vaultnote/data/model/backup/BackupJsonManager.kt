package com.lankeshwar.vaultnote.data.model.backup

import kotlinx.serialization.json.Json

object BackupJsonManager{
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    /**
     * Converts VaultNote backup data into JSON.
     */
    fun encodeBackup(backup: VaultNoteBackup): String{
        return json.encodeToString(backup)
    }

    /**
     * Converts backup JSON back into VaultNote backup data.
     */
    fun decodeBackup(jsonData: String): VaultNoteBackup {
        val backup = json.decodeFromString<VaultNoteBackup>(jsonData)

        require(backup.appName == "VaultNote"){
            "This backup does not belong to VaultNote."
        }
        require(backup.formatVersion == 1){
            "Unsupported VaultNote backup version."
        }
        return backup
    }


}