package com.lankeshwar.vaultnote.screens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppInfoScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppLockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.ChangePinScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.FingerprintUnlockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.PrivacySecurityScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.RecentlyDeletedScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.TagManagementScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel

private object Routes {
    const val NOTES = "notes"
    const val CREATE_NOTE = "create_note"
    const val EDIT_NOTE = "edit_note"
    const val NOTE_DETAIL = "note_detail"
    const val PRIVATE = "private"
    const val SETTINGS = "settings"
    const val PRIVACY_SECURITY = "privacy_security"
    const val CHANGE_PIN = "change_pin"
    const val FINGERPRINT_UNLOCK = "fingerprint_unlock"
    const val APP_LOCK = "app_lock"
    const val APP_INFO = "app_info"
    const val RECENTLY_DELETED = "recently_deleted"
    const val TAG_MANAGEMENT = "tag_management"
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MainScreen(selectTheme: AppTheme? = null,
               onThemeChange: (AppTheme) -> Unit = {}) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route
    var selectedNote by remember {
        mutableStateOf<Note?>(null)
    }

    Scaffold (
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.NOTES,
                    onClick = {
                        navController.navigate(Routes.NOTES){
                            popUpTo(navController.graph.startDestinationId){
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    icon = { Text("📝") },
                    label = {Text("Notes")}
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.PRIVATE,
                    onClick = {
                        navController.navigate(Routes.PRIVATE){
                            popUpTo(navController.graph.startDestinationId){
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    icon = { Text("🔒") },
                    label = {Text("Private")}
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = {
                        navController.navigate(Routes.SETTINGS){
                            popUpTo(navController.graph.startDestinationId){
                                inclusive = false
                            }
                            launchSingleTop = true

                        }
                    },
                    icon = { Text("⚙️") },
                    label = {Text("Settings")}
                )
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current
        NavHost(
            navController = navController,
            startDestination = Routes.NOTES,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(route = Routes.NOTES) {
                NotesScreen(
                  notesViewModel = notesViewModel,
                    onAddNote = {
                        navController.navigate(Routes.CREATE_NOTE)
                    },
                    onEditNote = {note ->
                        selectedNote = note
                        navController.navigate(Routes.NOTE_DETAIL)
                    }

                )
            }

            composable(route=Routes.CREATE_NOTE){
                CreateNoteScreen(
                    onSave = {title,content,formattedContent,imageUris, fileUris ->
                        notesViewModel.addNote(title,content,formattedContent=formattedContent,
                            onNoteSaved = { noteId ->
                                notesViewModel.saveImageAttachments(
                                    noteId = noteId,
                                    imageUris = imageUris
                                )
                                notesViewModel.saveFileAttachments(
                                    noteId = noteId,
                                    fileUris = fileUris
                                )
                            },
                            onComplete = {
                                notesViewModel.deleteDraft("new_note")
                                navController.popBackStack()
                            })
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onAutoSave = { title,content, formattedContent, isMarkdown ->
                        notesViewModel.saveDraft(
                            draftId = "new_note",
                            title = title,
                            content = content,
                            formattedContent = formattedContent,
                            isMarkdown = isMarkdown
                        )
                    },
                    onLoadDraft = {
                        notesViewModel.getDraft("new_note")
                    },
                    onClearDraft = {
                        notesViewModel.deleteDraft("new_note")
                    }

                )
            }

            composable(Routes.RECENTLY_DELETED) {
                RecentlyDeletedScreen(
                    onBack = {
                        navController.popBackStack()
                    },

                )
            }

            composable (Routes.EDIT_NOTE){
                val note = selectedNote
                if (note != null){
                    EditNoteScreen(
                        noteId = note.id,
                        initialTitle = note.title,
                        initialContent = note.preview,
                        initialFormattedContent = note.formattedContent,
                        onSave = {title, content,formattedContent,imageUris,fileUris ->
                            notesViewModel.updateNote(
                                note = note,
                                title = title,
                                content = content,
                                formattedContent = formattedContent,
                                onComplete = {
                                    notesViewModel.saveImageAttachments(
                                        noteId = note.id.toLong(),
                                        imageUris = imageUris
                                    )
                                    notesViewModel.saveFileAttachments(
                                        noteId = note.id.toLong(),
                                        fileUris = fileUris
                                    )

                                    notesViewModel.deleteDraft("edit_note_${note.id}")
                                    navController.popBackStack()
                                }
                            )
                        },
                        onAutoSave = {title,content, formattedContent,onComplete ->
                            notesViewModel.updateNote(
                                note = note,
                                title = title,
                                content = content,
                                formattedContent = formattedContent,
                                onComplete = {
                                    notesViewModel.deleteDraft("edit_note_${note.id}")
                                    onComplete()
                                }
                            )
                        },
                        onCancel = {
                            navController.popBackStack()
                        },
                        onAutoSaveDraft = {title,content,formattedContent,isMarkdown ->
                            notesViewModel.saveDraft(
                                draftId = "edit_note_${note.id}",
                                title = title,
                                content = content,
                                formattedContent = formattedContent,
                                isMarkdown = isMarkdown
                            )
                        },
                        onLoadDraft = { draftId ->
                            notesViewModel.getDraft(draftId)
                        },
                        onClearDraft = { draftId ->
                            notesViewModel.deleteDraft(draftId)
                        }
                    )
                }
            }

            composable(Routes.NOTE_DETAIL){
                val notes by notesViewModel.notesFlow.collectAsState()
                val note = notes.find {it.id == selectedNote?.id} ?: selectedNote
                if(note != null){
                    NoteDetailScreen(
                        noteId = note.id,
                        title = note.title,
                        content = note.preview,
                        formattedContent = note.formattedContent,
                        date = note.date,
                        onBack = {
                            navController.popBackStack()
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT,note.title)
                                putExtra(Intent.EXTRA_TEXT,"${note.title}\n\n${note.preview}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent,"Share note"))

                        },
                        onEdit = {
                            navController.navigate(Routes.EDIT_NOTE)
                        }
                    )
                }
            }

            composable(Routes.PRIVATE) {
                PrivateNotesScreen(
                    notesViewModel = notesViewModel,
                    onNoteClick = {note ->
                        selectedNote = note
                        navController.navigate(Routes.NOTE_DETAIL)
                    }
                )
            }
            composable(Routes.SETTINGS) {

                SettingsScreen(onThemeChange = onThemeChange,
                    onPrivacySecurityClick = {
                        navController.navigate(Routes.PRIVACY_SECURITY)
                    },
                    onAboutClick = {
                        navController.navigate(Routes.APP_INFO)
                    },
                    onRecentlyDeletedClick = {
                        navController.navigate(Routes.RECENTLY_DELETED)
                    },
                    onManageTagsClick = {
                        navController.navigate(Routes.TAG_MANAGEMENT)
                    }
                )

            }

            composable(Routes.PRIVACY_SECURITY){
                PrivacySecurityScreen(onPinClick = {
                    navController.navigate(Routes.CHANGE_PIN)
                },
                onFingerprintClick = {
                    navController.navigate(Routes.FINGERPRINT_UNLOCK)
                },
                onAppLockClick = {
                    navController.navigate(Routes.APP_LOCK)
                })
            }

            composable(Routes.CHANGE_PIN) {
                ChangePinScreen(onBack = {
                        navController.popBackStack()
                })
            }
            composable(Routes.FINGERPRINT_UNLOCK) {
                FingerprintUnlockScreen()
            }

            composable(Routes.APP_LOCK){
                AppLockScreen()
            }
            composable(Routes.APP_INFO) {
                AppInfoScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.TAG_MANAGEMENT){
                TagManagementScreen(onBack = {
                    navController.popBackStack()
                })
            }
        }
    }
}