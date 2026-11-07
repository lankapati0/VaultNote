package com.lankeshwar.vaultnote.screens.SettingSecurity.backup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackupRestoreMenuScreen(
    onCreateBackupClick:()-> Unit,
    onBackupHistoryClick:()-> Unit,
    onRestoreBackupClick:()-> Unit,
    onAutomaticBackupClick:()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Manage your VaultNote backups and restore your data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BackupMenuItem(
            title = "Create Backup",
            description = "Create a manual backup or save a local backup.",
            onClick = onCreateBackupClick
        )

        BackupMenuItem(
            title = "Backup History",
            description = "View, restore, preserve, or delete your local backups.",
            onClick = onBackupHistoryClick
        )

        BackupMenuItem(
            title = "Restore Backup",
            description = "Import and restore a backup file.",
            onClick = onRestoreBackupClick
        )
        BackupMenuItem(
            title = "Automatic Backup Settings",
            description = "Configure daily or weekly automatic backups.",
            onClick = onAutomaticBackupClick
        )
    }
}

@Composable
fun BackupMenuItem(title:String, description:String, onClick:()-> Unit){

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Column(                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text=description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text="›",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}