package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.Utils.AppLockPreferences

@Composable
fun AppLockScreen(){
    val context = LocalContext.current
    var appLockEnabled by remember {
        mutableStateOf(AppLockPreferences.isAppLockEnabled(context))
    }

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

}