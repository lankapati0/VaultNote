package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.Utils.AppLockPreferences
import com.lankeshwar.vaultnote.Utils.PinManager

@Composable
fun AppLockScreen(){
    val context = LocalContext.current
    var appLockEnabled by remember {
        mutableStateOf(AppLockPreferences.isAppLockEnabled(context))
    }
    var showDecoyPinDialog by remember {
        mutableStateOf(false)
    }

    var currentPin by remember { mutableStateOf("") }
    var newDecoyPin by remember { mutableStateOf("") }
    var confirmDecoyPin by remember { mutableStateOf("") }
    var decoyPinError by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "App Lock",
            style=MaterialTheme.typography.headlineMedium
        )

        Card(Modifier.fillMaxWidth()) {
            Row (
                Modifier.fillMaxWidth().padding(16.dp),
                Arrangement.spacedBy(12.dp),
                Alignment.CenterVertically
            ){
                Column(
                    Modifier.weight(1f),
                    Arrangement.spacedBy(6.dp)
                ) {
                    Text("Automatic Lock",
                        style = MaterialTheme.typography.titleMedium)

                    Text(
                        text = "Require PIN or fingerprint again when VaultNote goes into the background.",
                        style = MaterialTheme.typography.bodySmall
                        )
                }

                Switch(checked = appLockEnabled,
                    onCheckedChange = { enabled ->
                        appLockEnabled = enabled
                        AppLockPreferences.setAppLockEnabled(context,enabled)
                    })
            }
        }

        Spacer(Modifier.weight(1f))
        Card(
            Modifier.fillMaxWidth()
        ){
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Decoy PIN",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = if(PinManager.hasDecoyPin(context))
                        "A decoy PIN is configured."
                    else
                        "Set an alternative PIN to open an empty private area."
                )

                Button(
                    onClick = {
                        showDecoyPinDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if(PinManager.hasDecoyPin(context))
                        "Change Decoy Pin"
                        else
                            "Set Up Decoy Pin"
                    )
                }

            }
        }


        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ){
            Text(
                "Your saved notes and PIN won't be deleted when you change this setting.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }


    if(showDecoyPinDialog){
        AlertDialog(
            onDismissRequest = {
                showDecoyPinDialog = false
                currentPin = ""
                newDecoyPin = ""
                confirmDecoyPin = ""
                decoyPinError = ""
            },
            title = {
                Text(text="Setup Decoy Pin")
            },
            text= {
                Column (verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = {currentPin = it},
                        label = {Text("Current Real PIN")},
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )

                    OutlinedTextField(
                        value = newDecoyPin,
                        onValueChange = {newDecoyPin = it},
                        label = {Text("New Decoy PIN")},
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = confirmDecoyPin,
                        onValueChange = {confirmDecoyPin = it},
                        label = {Text("Confirm Decoy PIN")},
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )

                    if(decoyPinError.isNotEmpty()){
                        Text(
                            text = decoyPinError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when {
                        currentPin.isBlank() || newDecoyPin.isBlank() || confirmDecoyPin.isBlank() -> {
                            decoyPinError = "Please fill in all fields."
                        }
                        !PinManager.verifyPin(context,currentPin) -> {
                            decoyPinError = "Current real PIN is incorrect."
                        }

                        newDecoyPin.length !in 4..6 -> {
                            decoyPinError = "Decoy PIN must be 4 to 6 digits."
                        }
                        newDecoyPin.any {!it.isDigit()} -> {
                            decoyPinError = "PIN must contain only numbers."
                        }
                        newDecoyPin != confirmDecoyPin -> {
                            decoyPinError = "Decoy PINs do not match."
                        }
                        PinManager.isSameAsRealPin(context,newDecoyPin)->{
                            decoyPinError = "Decoy PIN must be different from your real PIN."
                        }
                        else -> {
                            PinManager.saveDecoyPin(context,newDecoyPin)
                            showDecoyPinDialog = false
                            currentPin = ""
                            newDecoyPin = ""
                            confirmDecoyPin = ""
                            decoyPinError = ""
                        }
                    }
                }) {Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDecoyPinDialog = false
                    currentPin = ""
                    confirmDecoyPin = ""
                    decoyPinError = ""
                    newDecoyPin = ""
                }) { Text("Cancel")}
            }
        )
    }
}