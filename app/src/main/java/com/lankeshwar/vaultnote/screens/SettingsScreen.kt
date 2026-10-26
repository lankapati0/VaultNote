package com.lankeshwar.vaultnote.screens

import android.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.theme.ThemePreferences


@Composable
fun SettingsScreen(onThemeChange:(AppTheme) -> Unit,
                   onPrivacySecurityClick: ()-> Unit,
                   onAboutClick:()-> Unit,
                   onRecentlyDeletedClick: () -> Unit) {

    var showAppearanceDialog by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "General",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsItem(
            title = "Appearance",
            subtitle = "Customize the app theme",
            icon = "🎨",
            onClick = {
                showAppearanceDialog = true
            }
        )

        SettingsItem(
            title = "Privacy & Security",
            subtitle = "Manage your private notes and screen lock",
            icon = "🔐",
            onClick = onPrivacySecurityClick
        )

        SettingsItem(
            title = "About VaultNote",
            subtitle = "App information and version",
            icon = "ℹ️",
            onClick = onAboutClick
        )
        SettingsItem(
            title = "Recently Deleted",
            onClick = onRecentlyDeletedClick,
            subtitle = "",
            icon = "\u21BA"
        )
    }

    if(showAppearanceDialog){
        androidx.compose.material3.AlertDialog(onDismissRequest = {
            showAppearanceDialog = false
        },
                title = {
                    Text("Choose appearance")
                },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            onThemeChange(AppTheme.LIGHT)
                            showAppearanceDialog = false
                        }
                    ) {
                        Text("Light")
                    }
                    TextButton(
                        onClick = {
                            onThemeChange(AppTheme.DARK)
                            showAppearanceDialog = false
                        }
                    ) {
                        Text("Dark")
                    }

                    TextButton(
                        onClick = {
                            onThemeChange(AppTheme.SYSTEM)
                            showAppearanceDialog = false
                        }
                    ) {
                        Text("System Default")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAppearanceDialog = false
                }) {
                    Text("Close")
                }
            }
            )
    }

}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: String, onClick:()-> Unit){
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clickable{onClick()},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ){
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon,
                style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text= ">",
                style = MaterialTheme.typography.headlineMedium

            )
        }
    }
}

