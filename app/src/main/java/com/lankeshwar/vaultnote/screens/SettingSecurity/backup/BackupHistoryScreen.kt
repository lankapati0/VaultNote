package com.lankeshwar.vaultnote.screens.SettingSecurity.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.viewmodels.backup.BackupViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupHistoryScreen(viewModel: BackupViewModel = hiltViewModel()){

    val localBackups by viewModel.localBackups.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()

    var expandedBackupMenu by remember { mutableStateOf<String?>(null) }
    var backupToDelete by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadLocalBackups()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        Text(
            text = "Manage the local backups saved on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Local Backups (${localBackups.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
        }

        if(localBackups.isEmpty()){
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "No local backups found.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Create a local backup from the Create Backup screen. It will appear here.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            localBackups.forEach { backupFile ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = backupFile.name.removePrefix("Preserved_"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()
                                ).format(
                                    Date(backupFile.lastModified())
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%.2f MB",
                                    backupFile.length() / (1024.0 * 1024.0)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (backupFile.name.startsWith("Preserved_")) {
                                Text(
                                    text = "Preserved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                expandedBackupMenu = backupFile.absolutePath
                            }
                        ) {
                            Text("Options")
                        }

                        DropdownMenu(
                            expanded = expandedBackupMenu == backupFile.absolutePath,
                            onDismissRequest = {
                                expandedBackupMenu = null
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (backupFile.name.startsWith("Preserved_")) {
                                            "Unpreserve"
                                        } else {
                                            "Preserve"
                                        }
                                    )
                                },
                                onClick = {
                                    expandedBackupMenu = null

                                    if (backupFile.name.startsWith("Preserved_")) {
                                        viewModel.unpreserveLocalBackup(backupFile)
                                    } else {
                                        viewModel.preserveLocalBackup(backupFile)
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Delete")
                                },
                                enabled = !backupFile.name.startsWith("Preserved_"),
                                onClick = {
                                    expandedBackupMenu = null
                                    backupToDelete = backupFile
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

                OutlinedButton(
                    onClick = {
                        viewModel.loadLocalBackups()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refresh History")
                }

                backupMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                backupToDelete = null
            },
            title = {
                Text("Delete Local Backup?")
            },
            text = {
                Text(
                    "This will permanently delete this backup file from your device. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backupToDelete?.let { file ->
                            viewModel.deleteLocalBackups(file)
                        }

                        backupToDelete = null
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        backupToDelete = null
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}