package com.lankeshwar.vaultnote

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.Utils.ocr.OcrLanguageManager
import com.lankeshwar.vaultnote.screens.MainScreen
import com.lankeshwar.vaultnote.screens.OcrSetupScreen
import com.lankeshwar.vaultnote.screens.SplashScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.theme.ThemePreferences
import com.lankeshwar.vaultnote.ui.theme.VaultNoteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val notificationNoteId = mutableIntStateOf(-1)
    private val widgetTaskId = mutableIntStateOf(-1)
    private val openCreateNote = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationNoteId.intValue = intent.getIntExtra("noteId",-1)
        widgetTaskId.intValue = intent.getIntExtra("taskId",-1)
        openCreateNote.value = intent.getBooleanExtra("openCreateNote", false)
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationNoteId.intValue = intent.getIntExtra("noteId",-1)
        widgetTaskId.intValue = intent.getIntExtra("taskId",-1)
        openCreateNote.value = intent.getBooleanExtra("openCreateNote", false)
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var showSplash by remember { mutableStateOf(true) }
            val ocrLanguageManager = remember{ OcrLanguageManager() }
            var isOcrSetupComplete by remember {mutableStateOf(false)}
            var ocrProgress by remember {mutableIntStateOf(0)}
            var currentOcrLanguage by remember {mutableStateOf("")}
            var ocrSetupFailed by remember { mutableStateOf(false) }

            var selectTheme by remember {
                mutableStateOf(ThemePreferences.getTheme(context))
            }

            val darkTheme = when (selectTheme){
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {

                val installed = ocrLanguageManager.isInitialLanguagePackInstalled(context)

                if(installed) {
                    isOcrSetupComplete = true
                    ocrSetupFailed = false

                }
                delay(2000.milliseconds)
                showSplash = false

                if(!installed){
                    val success = ocrLanguageManager.downloadInitialLanguagePack(context){progress, language ->
                        scope.launch(Dispatchers.Main){
                            ocrProgress = progress
                            currentOcrLanguage = language
                        }
                    }
                    if(success){
                        isOcrSetupComplete = true
                    } else {
                        ocrSetupFailed = true
                    }
                }
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
                if (showSplash) {
                    SplashScreen()
                } else if(ocrSetupFailed){
                    Text(
                        text = "OCR language download failed. Please check your internet connection and restart the app.",
                        modifier = Modifier.padding(24.dp)
                    )
                } else if(!isOcrSetupComplete){
                    OcrSetupScreen(
                        progress = ocrProgress, currentLanguage = currentOcrLanguage
                    )
                } else {
                    MainScreen(
                        selectTheme = selectTheme,
                        onThemeChange = {theme ->
                            ThemePreferences.saveTheme(context, theme)
                            selectTheme = theme
                        },
                        notificationNoteId = notificationNoteId.intValue.takeIf { it != -1 },
                        onNotificationHandled = { notificationNoteId.intValue = -1 },
                        widgetTaskId = widgetTaskId.intValue.takeIf { it != -1 },
                        onWidgetTaskHandled = {
                            widgetTaskId.intValue = -1
                        },
                        openCreateNote = openCreateNote.value,
                        onCreateNoteHandled = {
                            openCreateNote.value = false
                        }
                    )
                }
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