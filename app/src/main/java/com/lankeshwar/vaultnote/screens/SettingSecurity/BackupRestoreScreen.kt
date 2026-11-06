package com.lankeshwar.vaultnote.screens.SettingSecurity

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.screens.SettingsItem
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
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }

    val isCreatingBackup by viewModel.isCreatingBackup.collectAsState()
    val backupMessage by viewModel.backupMessage.collectAsState()

    val passwordMatch = password.isNotEmpty() && password == confirmPassword
    val isPasswordValid = password.length >= 6 && passwordMatch
    var showPinDialog by remember { mutableStateOf(false) }

    var isRestorePinFlow by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    var backupPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var restorePassword by remember { mutableStateOf("") }
    var showRestorePassword by remember { mutableStateOf(false) }
    var selectedRestoreFile by remember { mutableStateOf<File?>(null) }
    var isPreparingRestore by remember { mutableStateOf(false) }

    val restoreMessage by viewModel.isRestoringMessage.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val isRestoringBackup by viewModel.isRestoringBackup.collectAsState()

    val createDocumentLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            val savedPassword = pendingPassword
            pendingPassword = null
            if (uri != null && savedPassword != null) {
                viewModel.createBackup(uri, savedPassword)
            } else {
                savedPassword?.fill('\u0000')
                viewModel.cancelBackupPreparation()
            }
        }

    val openBackupLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {uri: Uri? ->
        if(uri != null){
            selectedRestoreFile?.delete()
            viewModel.cancelRestorePreparation()
            isPreparingRestore = true
            selectedRestoreFile = null
            restorePassword = ""

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
                            } ?: throw IllegalStateException("Unable to open the selected backup.")
                        file
                    }
                    selectedRestoreFile = backupFile
                }catch (e: Exception){
                    viewModel.clearRestoreMessage(e.message ?: "Unable to read the selected backup.")
                } finally {
                    isPreparingRestore = false
                }
            }
        }
    }

    val openBackupPicker: () -> Unit = {
        pendingPassword = password.toCharArray()
        password = ""
        confirmPassword = ""
        createDocumentLauncher.launch("VaultNote_Backup_${System.currentTimeMillis()}.vnb")
    }

    LaunchedEffect(restoreMessage,isRestoringBackup) {
        if(!isRestoringBackup && restoreMessage == "Backup restored successfully."){
            selectedRestoreFile = null
            restorePassword = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Backup & Restore",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Create a password-protected backup of your VaultNote data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Create Backup password") },
            singleLine = true,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            enabled = !isCreatingBackup,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(
                    onClick = { showPassword = !showPassword }
                ) { Text(if (showPassword) "Hide" else "Show") }
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm Backup Password") },
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
                    Text(if (showConfirmPassword) "Hide" else "Show")
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

        Button(
            onClick = {
                if (viewModel.requiresPinAuthentication()) {
                    pinError = ""
                    backupPin = ""
                    showPinDialog = true
                } else {
                    openBackupPicker()
                }
            },
            enabled = isPasswordValid && !isCreatingBackup && pendingPassword == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Backup")
        }

        if (isCreatingBackup) {
            CircularProgressIndicator()
            Text("Creating your encrypted backup...")
        }

        backupMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Keep your backup password safe. You will need the same password to restore your backup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()
        Text(text = "Restore Backup", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Select an existing VaultNote backup and enter its password to preview its contents.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = {
            openBackupLauncher.launch(arrayOf("*/*"))
        }, enabled = !isPreparingRestore && !isRestoringBackup,
            modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isPreparingRestore) {
                    "Loading backup file..."
                } else {
                    "Select Backup File"
                }
            )
        }

        selectedRestoreFile?.let { file ->
            Text(
                text = "Selected: ${file.name}",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(value = restorePassword,
                onValueChange = {restorePassword = it},
                label = { Text("Backup Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if(showRestorePassword){
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
                        Text(if (showRestorePassword) "Hide" else "Show")
                    }
                },
                enabled = !isRestoringBackup
            )

            Button(onClick = {
                viewModel.previewRestoreBackup(backupFile = file, password = restorePassword.toCharArray())
                restorePassword = ""
            },
                enabled = restorePassword.isNotEmpty() && !isRestoringBackup && backupPreview == null,
                modifier = Modifier.fillMaxWidth()

            ) {Text("Verify Backup") }

            restoreMessage?.let { message ->
                Text(
                    text = message,
                    color = if(backupPreview != null){
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            backupPreview?.let { preview ->
                Text(
                    text = "Backup Details",
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
                    text= "Created: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(Date(preview.createdAt))}"
                )

                Button(onClick = {
                    if(viewModel.requiresPinAuthentication()){
                        isRestorePinFlow = true
                        pinError = ""
                        backupPin = ""
                        showPinDialog = true
                    } else {
                        showRestoreConfirmDialog = true
                    }
                },
                    enabled = !isRestoringBackup,
                    modifier = Modifier.fillMaxWidth()) {Text("Restore Backup") }
            }
        }

    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAuthenticating) {
                    showPinDialog = false
                    pinError = ""
                    backupPin = ""

                    if(isRestorePinFlow){
                        isRestorePinFlow = false
                        viewModel.cancelRestorePreparation()
                        selectedRestoreFile = null
                    }
                }
            },
            title = {
                if(isRestorePinFlow){
                    Text("Verify PIN to restore")
                } else {
                    Text("Verify your PIN")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if(isRestorePinFlow){
                            "Enter your real VaultNote PIN to restore private notes and attachments."
                        } else {
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
                        viewModel.authenticateForBackup(backupPin) { success, message ->

                            if (success) {
                                showPinDialog = false
                                backupPin = ""
                                pinError = ""

                                if(isRestorePinFlow){
                                    isRestorePinFlow = false
                                    showRestoreConfirmDialog = true
                                } else {
                                    openBackupPicker()
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
                        if(isRestorePinFlow){
                            isRestorePinFlow = false
                            viewModel.cancelRestorePreparation()
                            selectedRestoreFile = null
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if(showRestoreConfirmDialog){
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
                Text("VaultNote will restore notes, attachments, tags, tasks and journal entries from this backup. Existing journal entries with matching dates will be kept, while matching tasks will be updated using backup data. Other existing data will be preserved. Continue?")
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

    if(isRestoringBackup){
        AlertDialog(
            onDismissRequest = {},
            title = {Text("Restoring Backup")},
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