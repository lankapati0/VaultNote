package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.screens.SettingsItem

@Composable
fun PrivacySecurityScreen(
    onPinClick:()-> Unit,
    onFingerprintClick: () -> Unit,
    onAppLockClick:()-> Unit
){
    Column(Modifier.padding(20.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Privacy & Security",
            style = MaterialTheme.typography.headlineMedium)

        SettingsItem(
            title = "Private Notes PIN",
            subtitle = "Change or manage your PIN",
            icon = "🔑",
            onClick = onPinClick
        )

        SettingsItem(
            title = "Fingerprint Unlock",
            subtitle = "Manage fingerprint preferences",
            icon = "👆",
            onClick = onFingerprintClick
        )

        SettingsItem(
            title = "App Lock",
            subtitle = "Manage your private notes lock",
            icon = "🛡️",
            onClick = onAppLockClick
        )
    }
}