package com.lankeshwar.vaultnote.screens.SettingSecurity.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.viewmodels.backup.BackupViewModel

@Composable
fun CreateBackupScreen(
    viewModel: BackupViewModel = hiltViewModel()
){
    val isCreating by viewModel.isCreatingBackup.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val message by viewModel.backupMessage.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var backupType by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf<CharArray?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val savedPassword = pendingPassword
        pendingPassword = null

        if (uri != null && savedPassword != null) {
            viewModel.createBackup(uri, savedPassword)
        } else {
            savedPassword?.fill('\u0000')
            viewModel.cancelBackupPreparation()
        }
    }

    fun beginBackup(type: String) {
        backupType = type
        pinError = null

        if (viewModel.requiresPinAuthentication()) {
            showPinDialog = true
        } else {
            showPasswordDialog = true
        }
    }

    fun authenticate() {
        viewModel.authenticateForBackup(enteredPin) { success, error ->
            if (success) {
                showPinDialog = false
                enteredPin = ""
                pinError = null
                showPasswordDialog = true
            } else {
                pinError = error
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Protect your notes by creating an encrypted backup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Manual Backup",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Save an encrypted backup to your chosen location.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { beginBackup("EXPORT") },
                    enabled = !isCreating && !isAuthenticating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Manual Backup")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Local Backup",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Save an encrypted backup inside VaultNote. You can manage it later from Backup History.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { beginBackup("LOCAL") },
                    enabled = !isCreating && !isAuthenticating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Local Backup")
                }
            }
        }

        if (isCreating || isAuthenticating) {
            CircularProgressIndicator()
        }

        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                enteredPin = ""
                pinError = null
                viewModel.cancelBackupPreparation()
            },
            title = { Text("Verify PIN") },
            text = {
                Column {
                    Text("Enter your real VaultNote PIN to continue.")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        singleLine = true
                    )

                    pinError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { authenticate() },
                    enabled = enteredPin.isNotBlank() && !isAuthenticating
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        enteredPin = ""
                        viewModel.cancelBackupPreparation()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                passwordError = null
                viewModel.cancelBackupPreparation()
            },
            title = { Text("Backup Password") },
            text = {
                Column {
                    Text("Create a password to encrypt this backup. You will need it when restoring.")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text("Backup password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    passwordError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isBlank()) {
                            passwordError = "Please enter a backup password."
                        } else {
                            val passwordChars = password.toCharArray()
                            password = ""
                            showPasswordDialog = false

                            if (backupType == "LOCAL") {
                                viewModel.createLocalBackup(passwordChars)
                            } else {
                                pendingPassword?.fill('\u0000')
                                pendingPassword = passwordChars
                                exportBackupLauncher.launch("VaultNote_Backup.vnb")
                            }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        viewModel.cancelBackupPreparation()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}