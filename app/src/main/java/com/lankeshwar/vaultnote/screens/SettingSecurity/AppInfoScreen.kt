package com.lankeshwar.vaultnote.screens.SettingSecurity

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AppInfoScreen(onBack:()-> Unit){
    val context = LocalContext.current
    val versionName = getAppVersion(context)

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
/*            IconButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "About VaultNote",
                style = MaterialTheme.typography.titleLarge
            )*/
        }

        Card(Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            ) {
            Column(Modifier.fillMaxWidth().padding(24.dp),
                Arrangement.spacedBy(12.dp),
                Alignment.CenterHorizontally) {

                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Lock,
                    "VaultNote app icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
                Text("VaultNote",
                    style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Your private notes, organized securely.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)){
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Information", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            Column(Modifier.padding(top = 12.dp),
                Arrangement.spacedBy(12.dp)) {
                InfoRow(
                    label =  "App Name",
                    value = "Vault Note"
                )
                InfoRow(
                    label =  "Version",
                    value = versionName
                )
                InfoRow(
                    label = "Package",
                    value = context.packageName
                )
                InfoRow(
                    label = "Platform",
                    value = "Android"
                )
            }

        }

    }
}

@Composable
private fun InfoRow(label: String, value: String){
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value,
            style = MaterialTheme.typography.bodyLarge)
    }

}

private fun getAppVersion(context: Context): String{
    return try {
        context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "Unknown"
    } catch (_: PackageManager.NameNotFoundException){
        "Unknown"
    }
}