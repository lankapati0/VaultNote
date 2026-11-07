package com.lankeshwar.vaultnote.screens.SettingSecurity.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

enum class AutomaticBackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}
@Composable
fun AutomaticBackupSettingsScreen(
    isEnabled:Boolean,
    frequency: AutomaticBackupFrequency,
    isPasswordConfigured: Boolean,
    lastBackupText: String,
    onEnabledChange: (Boolean) -> Unit,
    onFrequencyChange:(AutomaticBackupFrequency) -> Unit,
    onPasswordSave:(CharArray) -> Unit,
    modifier:Modifier = Modifier
) {
    val showPasswordDialog = remember { mutableStateOf(false) }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val passwordError = remember { mutableStateOf<String?>(null) }



    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "Keep your notes safe with scheduled encrypted backups.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        // Automatic backup switch
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Enable automatic backups",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Automatically create encrypted local backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange
                )

            }
            Spacer(modifier = Modifier.height(24.dp))

            // Backup frequency
            Text(
                text = "Backup frequency",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = frequency == AutomaticBackupFrequency.DAILY,
                    onClick = {
                        onFrequencyChange(AutomaticBackupFrequency.DAILY)
                    },
                    enabled = isEnabled,
                    label = { Text("Daily") }
                )
                FilterChip(
                    selected = frequency == AutomaticBackupFrequency.WEEKLY,
                    onClick = {
                        onFrequencyChange(AutomaticBackupFrequency.WEEKLY)
                    },
                    enabled = isEnabled,
                    label = { Text("Weekly") }
                )
                FilterChip(
                    selected = frequency == AutomaticBackupFrequency.MONTHLY,
                    onClick = {
                        onFrequencyChange(AutomaticBackupFrequency.MONTHLY)
                    },
                    enabled = isEnabled,
                    label = { Text("Monthly") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Backup password
            Text(
                text = "Backup password",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if(isPasswordConfigured){
                            "Password configured"
                        } else {
                            "Password not configured"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your backup password is stored in encrypted form.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            password.value = ""
                            confirmPassword.value = ""
                            passwordError.value = ""
                            showPasswordDialog.value = true
                        }
                    ) {
                        Text(
                            if (isPasswordConfigured) {
                                "Change password"
                            } else {
                                "Set backup password"
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Retention information
            Text(
                text = "Backup retention",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()){
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Keep latest 5 automatic backups",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Older automatic backups will be deleted automatically. Preserved backups are never deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))
            // Last backup information
            Text(
                text = "Last automatic backup",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = lastBackupText,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if(showPasswordDialog.value){
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog.value = false
            },
            title = {
                Text(
                    if (isPasswordConfigured) {
                        "Change backup password"
                    } else {
                        "Set backup password"
                    }
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = {
                            password.value = it
                        },
                        label = { Text("Backup password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword.value,
                        onValueChange = {
                            confirmPassword.value = it
                        },
                        label = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    passwordError.value?.let {
                        Spacer(modifier = Modifier.height(8.dp))
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
                        when {
                            password.value.isBlank() -> {
                                passwordError.value = "Enter a password."
                            }

                            password.value != confirmPassword.value -> {
                                passwordError.value = "Passwords do not match."
                            }

                            else -> {
                                val passwordChars = password.value.toCharArray()

                                try {
                                    onPasswordSave(passwordChars)
                                    showPasswordDialog.value = false
                                    password.value = ""
                                    confirmPassword.value = ""
                                    passwordError.value = null
                                } finally {
                                    passwordChars.fill('\u0000')
                                }
                            }
                        }
                    }){
                    Text("Save")
                }

                        },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog.value = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}