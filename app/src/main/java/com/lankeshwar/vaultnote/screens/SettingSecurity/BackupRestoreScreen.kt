package com.lankeshwar.vaultnote.screens.SettingSecurity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.viewmodels.BackupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }

    val isCreatingBackup by viewModel.isCreatingBackup.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()
    val localBackups by viewModel.localBackups.collectAsState()

    var backupAction by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var backupPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

    var restorePassword by remember { mutableStateOf("") }
    var showRestorePassword by remember { mutableStateOf(false) }
    var selectedRestoreFile by remember { mutableStateOf<File?>(null) }
    var isPreparingRestore by remember { mutableStateOf(false) }

    val restoreMessage by viewModel.isRestoringMessage.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val isRestoringBackup by viewModel.isRestoringBackup.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteBackupDialog by remember { mutableStateOf(false) }
    var backupToDelete by remember { mutableStateOf<File?>(null) }

    var expandedBackupMenu by remember { mutableStateOf<String?>(null) }

    val passwordMatch =
        password.isNotEmpty() && password == confirmPassword

    val isPasswordValid =
        password.length >= 6 && passwordMatch

    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "application/octet-stream"
            )
        ) { uri ->
            val savedPassword = pendingPassword
            pendingPassword = null

            if (uri != null && savedPassword != null) {
                viewModel.createBackup(uri, savedPassword)
            } else {
                savedPassword?.fill('\u0000')
                viewModel.cancelBackupPreparation()
            }
        }

    val openBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {
                selectedRestoreFile?.delete()
                viewModel.cancelRestorePreparation()

                selectedRestoreFile = null
                restorePassword = ""
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

    LaunchedEffect(Unit) {
        viewModel.loadLocalBackups()
    }

    LaunchedEffect(backupMessage) {
        if (backupMessage == "Local backup created successfully.") {
            viewModel.loadLocalBackups()
        }
    }

    LaunchedEffect(restoreMessage, isRestoringBackup) {
        if (
            !isRestoringBackup &&
            restoreMessage == "Backup restored successfully."
        ) {
            selectedRestoreFile = null
            restorePassword = ""
        }
    }

    fun startBackup(action: String) {
        if (!isPasswordValid || isCreatingBackup) return

        backupAction = action

        if (viewModel.requiresPinAuthentication()) {
            backupPin = ""
            pinError = ""
            showPinDialog = true
        } else {
            if (action == "MANUAL") {
                pendingPassword = password.toCharArray()
                password = ""
                confirmPassword = ""

                createDocumentLauncher.launch(
                    "VaultNote_Backup_${System.currentTimeMillis()}.vnb"
                )
            } else {
                val localPassword = password.toCharArray()
                password = ""
                confirmPassword = ""

                viewModel.createLocalBackup(localPassword)
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

        // BACKUP CARD

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
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
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Backup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Create a password-protected copy of your VaultNote data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                Text(
                    text = "Backup Password",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Create password") },
                    singleLine = true,
                    enabled = !isCreatingBackup,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showPassword = !showPassword
                            }
                        ) {
                            Text(if (showPassword) "Hide" else "Show")
                        }
                    }
                )

                if (password.isNotEmpty() && password.length < 6) {
                    Text(
                        text = "Password must contain at least 6 characters.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm password") },
                    singleLine = true,
                    enabled = !isCreatingBackup,
                    visualTransformation = if (showConfirmPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showConfirmPassword = !showConfirmPassword
                            }
                        ) {
                            Text(
                                if (showConfirmPassword) "Hide" else "Show"
                            )
                        }
                    }
                )

                if (confirmPassword.isNotEmpty() && !passwordMatch) {
                    Text(
                        text = "Passwords do not match.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Choose how you want to save your backup.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        startBackup("MANUAL")
                    },
                    enabled = isPasswordValid &&
                            !isCreatingBackup &&
                            pendingPassword == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Manual Backup")
                }

                OutlinedButton(
                    onClick = {
                        startBackup("LOCAL")
                    },
                    enabled = isPasswordValid &&
                            !isCreatingBackup &&
                            pendingPassword == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Local Backup")
                }

                Text(
                    text = "Manual Backup lets you choose where to save the file. Local Backup saves it inside VaultNote on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCreatingBackup) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = "Creating your backup...",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                backupMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Keep your backup password safe. You will need it to restore your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // BACKUP HISTORY CARD

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Backup History",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = "Local backups stored on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (localBackups.isEmpty()) {
                    Text(
                        text = "No local backups yet.",
                        style = MaterialTheme.typography.bodyMedium,
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

                                Text(
                                    text = String.format(
                                        Locale.getDefault(),
                                        "%.2f MB",
                                        backupFile.length() / (1024.0 * 1024.0)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                    text = { Text("Restore") },
                                    onClick = {
                                        expandedBackupMenu = null
                                        selectedRestoreFile = backupFile
                                        restorePassword = ""
                                        viewModel.cancelRestorePreparation()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = null
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        expandedBackupMenu = null
                                        backupToDelete = backupFile
                                        showDeleteBackupDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null
                                        )
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
                    Text("Refresh History")
                }
            }
        }

        // RESTORE BACKUP CARD

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Restore Backup",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = "Restore data from a local backup or an exported VaultNote backup file.",
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
                    Text(
                        if (isPreparingRestore) {
                            "Loading backup file..."
                        } else {
                            "Select Backup File"
                        }
                    )
                }

                selectedRestoreFile?.let { file ->

                    Divider()

                    Text(
                        text = "Selected Backup",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = {
                            restorePassword = it
                        },
                        label = { Text("Backup Password") },
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
                            viewModel.previewRestoreBackup(
                                backupFile = file,
                                password = restorePassword.toCharArray()
                            )
                            restorePassword = ""
                        },
                        enabled = restorePassword.isNotEmpty() &&
                                !isRestoringBackup &&
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

                        Button(
                            onClick = {
                                if (viewModel.requiresPinAuthentication()) {
                                    backupAction = "RESTORE"
                                    backupPin = ""
                                    pinError = ""
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
                }

                if (isPreparingRestore) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // PIN AUTHENTICATION DIALOG

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAuthenticating) {
                    showPinDialog = false
                    backupPin = ""
                    pinError = ""
                    backupAction = ""
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
                        when (backupAction) {
                            "RESTORE" ->
                                "Enter your real VaultNote PIN to restore private notes and attachments."

                            else ->
                                "Enter your real VaultNote PIN to include private notes in this backup."
                        }
                    )

                    OutlinedTextField(
                        value = backupPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                backupPin = it
                                pinError = ""
                            }
                        },
                        label = { Text("VaultNote PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        enabled = !isAuthenticating
                    )

                    if (pinError.isNotEmpty()) {
                        Text(
                            text = pinError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
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
                                pinError = ""

                                when (backupAction) {
                                    "MANUAL" -> {
                                        pendingPassword = password.toCharArray()
                                        password = ""
                                        confirmPassword = ""

                                        createDocumentLauncher.launch(
                                            "VaultNote_Backup_${System.currentTimeMillis()}.vnb"
                                        )
                                    }

                                    "LOCAL" -> {
                                        val localPassword = password.toCharArray()
                                        password = ""
                                        confirmPassword = ""

                                        viewModel.createLocalBackup(localPassword)
                                    }

                                    "RESTORE" -> {
                                        showRestoreConfirmDialog = true
                                    }
                                }
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
                        pinError = ""
                        backupAction = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // RESTORE CONFIRMATION DIALOG

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                viewModel.cancelRestorePreparation()
                selectedRestoreFile = null
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
                        selectedRestoreFile = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE LOCAL BACKUP DIALOG

    if (showDeleteBackupDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteBackupDialog = false
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
                        backupToDelete?.let {
                            viewModel.deleteLocalBackups(it)
                        }

                        backupToDelete = null
                        showDeleteBackupDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteBackupDialog = false
                        backupToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // RESTORE PROGRESS DIALOG

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
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}