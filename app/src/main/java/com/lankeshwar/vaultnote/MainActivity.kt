package com.lankeshwar.vaultnote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.lankeshwar.vaultnote.screens.MainScreen
import com.lankeshwar.vaultnote.ui.theme.VaultNoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VaultNoteTheme {
        MainScreen()
    }
}