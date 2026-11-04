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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.Utils.BiometricHelper
import com.lankeshwar.vaultnote.Utils.BiometricPreferences
import com.lankeshwar.vaultnote.Utils.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChangePinScreen(onBack:()-> Unit){
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isResettingPin by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isSavingPin by remember { mutableStateOf(false) }
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

        Text(if (isResettingPin) "Reset Private Notes PIN" else "Private Notes PIN",
            style = MaterialTheme.typography.headlineMedium)

        Text(if (isResettingPin)
            "Enter your new PIN and confirm it."
        else
            "Enter your current PIN, then choose a new one.")

        if(!isResettingPin){
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
        }


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

        if(activity != null && BiometricPreferences.isFingerprintEnabled(context) &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
            BiometricHelper.canAuthenticate(activity)){
            TextButton(
                onClick = {
                    BiometricHelper.authenticate(activity=activity, title = "Reset Pin","Verify your fingerprint to create a new PIN",
                        onSucess = {
                            isResettingPin = true
                            currentPin = ""
                            newPin = ""
                            confirmPin = ""
                            message = ""
                            showAlert = false
                        },
                        onError = { error ->
                            message = error
                            isPinChangeSuccessful = false
                        })
                },
                modifier = Modifier.fillMaxWidth()
            ) {Text("Forgot PIN?") }
        }

        Button(onClick = {
            val validationError = when {
                !isResettingPin && currentPin.isBlank() -> "Please enter your current PIN."

                !isResettingPin && currentPin.length != 6 ->
                    "Current PIN must be 6 digits."

                newPin.length != 6 ->
                    "New PIN must be 6 digits."

                confirmPin.isBlank() ->
                    "Please confirm your new PIN."

                newPin != confirmPin ->
                    "New PINs do not match."

                else -> null
            }

            if(validationError != null){
                message = validationError
                isPinChangeSuccessful = false
                showAlert = true
            } else {
                isSavingPin = true
                coroutineScope.launch {
                    val isCurrentPinValid = if(isResettingPin){true} else {
                        withContext(Dispatchers.Default){
                            PinManager.verifyPin(context,currentPin)
                        }
                    }

                    if(!isCurrentPinValid){
                        message = "Current PIN is incorrect."
                        isPinChangeSuccessful = false
                        showAlert = true
                    } else {
                        withContext(Dispatchers.Default) {
                            PinManager.savePin(context, newPin)
                        }

                        PinManager.resetFailedAttempts(context)

                        currentPin = ""
                        newPin = ""
                        confirmPin = ""

                        message = if (isResettingPin) {
                            "Your PIN has been reset successfully."
                        } else {
                            "Your PIN has been changed successfully."
                        }

                        isPinChangeSuccessful = true
                        isResettingPin = false
                        showAlert = true
                    }
                    isSavingPin = false
                }
            }
        },
            enabled = !isSavingPin,
            modifier = Modifier.fillMaxWidth()) {
            Text(when {
                isSavingPin -> "Saving..."
                isResettingPin -> "Reset PIN"
                else -> "Change PIN"
            })
        }
    }



    if (showAlert) {
        AlertDialog(
            onDismissRequest = {
                showAlert = false
            },
            title = {
                Text(
                    if (isPinChangeSuccessful) "PIN Updated"
                    else "Unable to Update PIN"
                )
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlert = false

                        if (isPinChangeSuccessful) {
                            onBack()
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

}
