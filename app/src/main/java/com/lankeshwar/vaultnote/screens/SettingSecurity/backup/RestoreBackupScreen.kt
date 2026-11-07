package com.lankeshwar.vaultnote.screens.SettingSecurity.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.viewmodels.backup.BackupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RestoreBackupScreen(viewModel: BackupViewModel = hiltViewModel()){

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val localBackups by viewModel.localBackups.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val restoreMessage by viewModel.isRestoringMessage.collectAsState()
    val isRestoringBackup by viewModel.isRestoringBackup.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

    var selectedRestoreFile by remember {
        mutableStateOf<File?>(null)
    }

    var selectedFileIsTemporary by remember {
        mutableStateOf(false)
    }

    var restorePassword by remember {
        mutableStateOf("")
    }

    var showRestorePassword by remember {
        mutableStateOf(false)
    }

    var isPreparingRestore by remember {
        mutableStateOf(false)
    }

    var showPinDialog by remember {
        mutableStateOf(false)
    }

    var backupPin by remember {
        mutableStateOf("")
    }

    var pinError by remember {
        mutableStateOf<String?>(null)
    }

    var showRestoreConfirmDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.loadLocalBackups()
    }

    fun clearSelectedFile() {
        if (selectedFileIsTemporary) {
            selectedRestoreFile?.delete()
        }

        selectedRestoreFile = null
        selectedFileIsTemporary = false
        restorePassword = ""
        viewModel.cancelRestorePreparation()
    }

    fun selectLocalBackup(file: File) {
        clearSelectedFile()
        selectedRestoreFile = file
        selectedFileIsTemporary = false
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            clearSelectedFile()
            isPreparingRestore = true

            scope.launch {
                try {
                    val backupFile = withContext(Dispatchers.IO) {
                        val file = File(
                            context.cacheDir,
                            "VaultNote_Restore_${System.currentTimeMillis()}.vnb"
                        )

                        context.contentResolver
                            .openInputStream(uri)
                            ?.use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            ?: throw IllegalStateException(
                                "Unable to open the selected backup."
                            )

                        file
                    }

                    selectedRestoreFile = backupFile
                    selectedFileIsTemporary = true

                } catch (e: Exception) {
                    viewModel.clearRestoreMessage(
                        e.message ?: "Unable to read the selected backup."
                    )
                } finally {
                    isPreparingRestore = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose a backup, verify its password, and restore your VaultNote data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // LOCAL BACKUPS

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
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
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Local Backups",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                if (localBackups.isEmpty()) {
                    Text(
                        text = "No local backups found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    localBackups.forEach { backupFile ->

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = backupFile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
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
                            }

                            TextButton(
                                enabled = !isRestoringBackup && !isPreparingRestore,
                                onClick = {
                                    selectLocalBackup(backupFile)
                                }
                            ) {
                                Text(
                                    if (selectedRestoreFile == backupFile) {
                                        "Selected"
                                    } else {
                                        "Select"
                                    }
                                )
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
                    Text("Refresh Local Backups")
                }
            }
        }

        // EXPORTED BACKUP FILE

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "External Backup File",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Select an exported VaultNote backup file from your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        openBackupLauncher.launch(arrayOf("*/*"))
                    },
                    enabled = !isPreparingRestore && !isRestoringBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Backup File")
                }

                if (isPreparingRestore) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text("Loading backup file...")
                }
            }
        }

        // SELECTED BACKUP AND PASSWORD

        selectedRestoreFile?.let { file ->

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selected Backup",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = {
                            restorePassword = it
                        },
                        label = {
                            Text("Backup Password")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRestoringBackup,
                        visualTransformation = if (showRestorePassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    showRestorePassword = !showRestorePassword
                                }
                            ) {
                                Text(
                                    if (showRestorePassword) "Hide" else "Show"
                                )
                            }
                        }
                    )

                    Button(
                        onClick = {
                            viewModel.clearRestoreMessage()

                            viewModel.previewRestoreBackup(
                                backupFile = file,
                                password = restorePassword.toCharArray()
                            )

                            restorePassword = ""
                        },
                        enabled = restorePassword.isNotEmpty() &&
                                !isRestoringBackup &&
                                !isPreparingRestore &&
                                backupPreview == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Backup")
                    }

                    restoreMessage?.let { message ->
                        Text(
                            text = message,
                            color = if (backupPreview != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    backupPreview?.let { preview ->

                        Divider()

                        Text(
                            text = "Backup Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Notes: ${preview.noteCount}")
                        Text("Private notes: ${preview.privateNoteCount}")
                        Text("Attachments: ${preview.attachmentCount}")
                        Text("Tags: ${preview.tagCount}")
                        Text("Drafts: ${preview.draftCount}")
                        Text("Tasks: ${preview.taskCount}")
                        Text("Journal entries: ${preview.journalEntryCount}")
                        Text("Journal photos: ${preview.journalPhotoCount}")

                        Text(
                            text = "Created: ${
                                SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a",
                                    Locale.getDefault()
                                ).format(Date(preview.createdAt))
                            }",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Divider()

                        Button(
                            onClick = {
                                if (viewModel.requiresPinAuthentication()) {
                                    backupPin = ""
                                    pinError = null
                                    showPinDialog = true
                                } else {
                                    showRestoreConfirmDialog = true
                                }
                            },
                            enabled = !isRestoringBackup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore This Backup")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            clearSelectedFile()
                        },
                        enabled = !isRestoringBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Selection")
                    }
                }
            }
        }

        restoreMessage?.let { message ->
            if (selectedRestoreFile == null) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // PIN AUTHENTICATION

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAuthenticating) {
                    showPinDialog = false
                    backupPin = ""
                    pinError = null
                }
            },
            title = {
                Text("Verify your PIN")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Enter your real VaultNote PIN to restore private notes and attachments."
                    )

                    OutlinedTextField(
                        value = backupPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                backupPin = it
                                pinError = null
                            }
                        },
                        label = {
                            Text("VaultNote PIN")
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        enabled = !isAuthenticating
                    )

                    pinError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (isAuthenticating) {
                        CircularProgressIndicator()
                        Text("Verifying PIN...")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = backupPin.length in 4..6 && !isAuthenticating,
                    onClick = {
                        viewModel.authenticateForBackup(backupPin) {
                                success, message ->

                            if (success) {
                                showPinDialog = false
                                backupPin = ""
                                pinError = null
                                showRestoreConfirmDialog = true
                            } else {
                                pinError = message ?: "PIN verification failed."
                            }
                        }
                    }
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isAuthenticating,
                    onClick = {
                        showPinDialog = false
                        backupPin = ""
                        pinError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
// RESTORE CONFIRMATION

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                viewModel.cancelRestorePreparation()
            },
            title = {
                Text("Confirm Backup Restoration")
            },
            text = {
                Text(
                    "VaultNote will restore notes, attachments, tags, tasks and journal entries from this backup. Existing journal entries with matching dates will be kept, while matching tasks will be updated using backup data. Other existing data will be preserved. Continue?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isRestoringBackup,
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreConfirmedBackup()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.cancelRestorePreparation()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // RESTORE PROGRESS

    if (isRestoringBackup) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Restoring Backup")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = "Please wait while VaultNote restores your data. This may take some time.",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // RESTORE RESULT

    LaunchedEffect(restoreMessage, isRestoringBackup) {
        if (
            !isRestoringBackup &&
            restoreMessage == "Backup restored successfully."
        ) {
            if (selectedFileIsTemporary) {
                selectedRestoreFile?.delete()
            }

            selectedRestoreFile = null
            selectedFileIsTemporary = false
            restorePassword = ""
            viewModel.loadLocalBackups()
        }
    }
}