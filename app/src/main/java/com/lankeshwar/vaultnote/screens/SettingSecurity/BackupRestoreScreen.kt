package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.screens.SettingsItem
import com.lankeshwar.vaultnote.viewmodels.BackupViewModel
import java.io.File

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
    var backupPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()

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

    val openBackupPicker: () -> Unit = {
        pendingPassword = password.toCharArray()
        password = ""
        confirmPassword = ""
        createDocumentLauncher.launch("VaultNote_Backup_${System.currentTimeMillis()}.vnb")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
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

        Text(
            text = "Restore functionality will be added separately.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAuthenticating) {
                    showPinDialog = false
                    pinError = ""
                    backupPin = ""
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
                        "Enter your real VaultNote PIN to include private notes in this backup."
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

                                openBackupPicker()
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
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}