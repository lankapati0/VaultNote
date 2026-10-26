package com.lankeshwar.vaultnote.screens.SettingSecurity

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.Utils.BiometricHelper
import com.lankeshwar.vaultnote.Utils.BiometricPreferences

@SuppressLint("RememberReturnType")
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun FingerprintUnlockScreen() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricAvailable = remember {
        activity != null && BiometricHelper.canAuthenticate(activity)
    }

    var fingerprintEnabled by remember {
        mutableStateOf(BiometricPreferences.isFingerprintEnabled(context))
    }

    Column(
        Modifier.padding(20.dp).fillMaxSize(),
        Arrangement.spacedBy(20.dp)
    ) {
        Text("Fingerprint Unlock",
            style=MaterialTheme.typography.headlineMedium)
        Text("Unlock your Private Notes quickly and securely.", style = MaterialTheme.typography.bodyLarge)

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Enable fingerprint",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (biometricAvailable) {
                            "Allow fingerprint access to Private Notes."
                        } else {
                            "Fingerprint authentication is unavailable on this device."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = fingerprintEnabled && biometricAvailable,
                    onCheckedChange = { enabled ->
                        fingerprintEnabled = enabled
                        BiometricPreferences.setFingerprintEnabled(
                            context,
                            enabled
                        )
                    },
                    enabled = biometricAvailable
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "🔒 Your PIN will continue to work even when fingerprint unlock is disabled.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}