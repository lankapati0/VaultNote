package com.lankeshwar.vaultnote

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.screens.MainScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.theme.ThemePreferences
import com.lankeshwar.vaultnote.ui.theme.VaultNoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            VaultNoteTheme(darkTheme = darkTheme) {
                MainScreen(
                    selectTheme = selectTheme,
                    onThemeChange = {theme ->
                        ThemePreferences.saveTheme(context, theme)
                        selectTheme = theme
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