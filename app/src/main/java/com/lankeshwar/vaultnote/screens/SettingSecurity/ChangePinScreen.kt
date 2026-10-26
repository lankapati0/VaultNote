package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.Utils.PinManager

@Composable
fun ChangePinScreen(onBack:()-> Unit){
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var isPinChangeSuccessful by remember { mutableStateOf(false) }

    Column(Modifier.padding(12.dp).fillMaxSize(),Arrangement.spacedBy(12.dp)) {

        TextButton(onClick = onBack){
            Text("← Back")
        }

        Text("Private Notes PIN",
            style =MaterialTheme.typography.headlineMedium)

        Text("Enter your current PIN, then choose a new one.")

        OutlinedTextField(
            value = currentPin,
            onValueChange = {
                if(it.length <=6 && it.all(Char::isDigit)) currentPin = it
            },
            label = {Text("Current PIN")},
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if(it.length <=6 && it.all(Char::isDigit)) newPin = it
            },
            label = {Text("New PIN (6 digits)")},
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if(it.length <=6 && it.all(Char::isDigit)) confirmPin = it
            },
            label = {Text("Confirm new PIN")},
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        if(message.isNotBlank()){
            Text(message)
        }

        Button(onClick = {
            when{
                currentPin.isBlank() || newPin.isBlank() || confirmPin.isBlank() -> {
                    message = "Please fill in all fields."
                    isPinChangeSuccessful = false
                }

                currentPin.length != 6 -> {
                    message = "Your current PIN must contain 6 digits."
                    isPinChangeSuccessful = false
                }
                newPin.length != 6 -> {
                    message = "Your new PIN must contain 6 digits."
                    isPinChangeSuccessful = false
                }

                newPin != confirmPin -> {
                    message = "The new PINs does not match."
                    isPinChangeSuccessful = false
                }

                !PinManager.verifyPin(context,currentPin) -> {
                    message = "Current PIN is incorrect."
                    isPinChangeSuccessful = false
                }
                else -> {
                    PinManager.savePin(context,newPin)
                    PinManager.resetFailedAttempts(context)
                    currentPin = ""
                    newPin = ""
                    confirmPin = ""
                    message = "Current PIN verified."
                    isPinChangeSuccessful = true
                }
            }
            showAlert = true
        },
            modifier = Modifier.fillMaxWidth()) {
            Text("Change PIN")
        }
    }
    if(showAlert){
        AlertDialog(
            onDismissRequest = {
                showAlert = false
                if(!isPinChangeSuccessful){
                    onBack()
                }
            },
            title = {
                if(isPinChangeSuccessful){
                    Text("Pin Updated")
                } else {
                    "Error"
                }
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlert = false
                        if(isPinChangeSuccessful){
                            onBack()
                        }
                    }
                ) {Text("Ok") }
            }
        )
    }
}