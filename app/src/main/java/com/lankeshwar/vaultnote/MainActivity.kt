package com.lankeshwar.vaultnote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.screens.MainScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.theme.ThemePreferences
import com.lankeshwar.vaultnote.ui.theme.VaultNoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val notificationNoteId = mutableIntStateOf(-1)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNoteId.intValue = intent.getIntExtra("noteId",-1)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationNoteId.intValue = intent.getIntExtra("noteId",-1)

        setContent {
            val context = LocalContext.current

            var selectTheme by remember {
                mutableStateOf(ThemePreferences.getTheme(context))
            }

            val darkTheme = when (selectTheme){
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->

            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
            }

            VaultNoteTheme(darkTheme = darkTheme) {
                MainScreen(
                    selectTheme = selectTheme,
                    onThemeChange = {theme ->
                        ThemePreferences.saveTheme(context, theme)
                        selectTheme = theme
                    },
                    notificationNoteId = notificationNoteId.intValue.takeIf { it != -1 },
                    onNotificationHandled = {
                        notificationNoteId.intValue = -1
                    }
                )
            }

        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val context = LocalContext.current
    val savedTheme = ThemePreferences.getTheme(context)
    VaultNoteTheme(
        darkTheme = when(savedTheme){
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
            AppTheme.SYSTEM -> isSystemInDarkTheme()
        }
    ) {
        MainScreen()
    }
}