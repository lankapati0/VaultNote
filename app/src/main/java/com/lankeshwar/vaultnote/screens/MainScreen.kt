package com.lankeshwar.vaultnote.screens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppInfoScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppLockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.ChangePinScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.FingerprintUnlockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.PrivacySecurityScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.RecentlyDeletedScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.TagManagementScreen
import com.lankeshwar.vaultnote.screens.tasks.TaskEditorScreen
import com.lankeshwar.vaultnote.screens.tasks.TaskListScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import kotlinx.coroutines.launch

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
    const val TASKS = "tasks"
    const val CREATE_TASK = "create_task"
    const val EDIT_TASK = "edit_task"
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MainScreen(selectTheme: AppTheme? = null,
               onThemeChange: (AppTheme) -> Unit = {},
               notificationNoteId:Int? = null,
               onNotificationHandled:()-> Unit = {}) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route

    val notes by notesViewModel.notesFlow.collectAsState()

    var handleNotificationNoteId by remember { mutableStateOf<Int?>(null) }
    var selectedNote by remember {
        mutableStateOf<Note?>(null)
    }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(notificationNoteId,notes) {
        if (notificationNoteId != null) {
            val targetNote = notes.find {
                it.id == notificationNoteId
            }

            if (targetNote != null) {
                selectedNote = targetNote

                navController.navigate(Routes.NOTE_DETAIL) {
                    popUpTo(Routes.NOTES) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }

                onNotificationHandled()
            }
        }

    }

    val shareNotes:()-> Unit = {
        val note = notes.find { it.id == selectedNote?.id } ?: selectedNote
        if (note != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${note.title}\n\n${note.preview}"
                )
            }

            context.startActivity(
                Intent.createChooser(shareIntent, "Share note")
            )
        }
    }
    ModalNavigationDrawer(

        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {

                NavigationDrawerItem(
                    label = {Text("Notes")},
                    selected = currentRoute == Routes.NOTES,
                    icon = { Text("📝") },
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.NOTES){
                                launchSingleTop = true
                            }
                        }
                    }
                )

                NavigationDrawerItem(
                    label = { Text("To-do") },
                    selected = currentRoute == Routes.TASKS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.TASKS) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Text("☑️") }
                )

                NavigationDrawerItem(
                    label = { Text("Private") },
                    selected = currentRoute == Routes.PRIVATE,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.PRIVATE) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Text("🔒") }
                )

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Text("⚙️") }
                )

/*                NavigationDrawerItem(
                    label = { Text("Recently Deleted") },
                    selected = currentRoute == Routes.RECENTLY_DELETED,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.RECENTLY_DELETED) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Text("🗑️") }
                )*/
            }
        }
    ) {
        val screenTitle = when(currentRoute){
            Routes.NOTES -> "VaultNote"
            Routes.TASKS -> "To-do"
            Routes.PRIVATE -> "Private"
            Routes.SETTINGS -> "Settings"
            Routes.RECENTLY_DELETED -> "Recently Deleted"
            Routes.CREATE_NOTE -> "Create Note"
            Routes.EDIT_NOTE -> "Edit Note"
            Routes.NOTE_DETAIL -> "Note Detail"
            Routes.CREATE_TASK -> "Create Task"
            Routes.EDIT_TASK -> "Edit Task"
            Routes.PRIVACY_SECURITY -> "Privacy & Security"
            Routes.CHANGE_PIN -> "Change PIN"
            Routes.FINGERPRINT_UNLOCK -> "Fingerprint Unlock"
            Routes.APP_LOCK -> "App Lock"
            Routes.APP_INFO -> "About VaultNote"
            Routes.TAG_MANAGEMENT -> "Manage Tags"
            else -> "VaultNote"
        }
    Scaffold (
        topBar = {
            TopAppBar(
                title = {Text(text = screenTitle) },
                navigationIcon = {

                    if(
                        currentRoute == Routes.NOTES ||
                        currentRoute == Routes.TASKS ||
                        currentRoute == Routes.PRIVATE ||
                        currentRoute == Routes.SETTINGS
                    ){
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(imageVector = Icons.Default.Menu,contentDescription = "Open navigation menu")
                        }
                    } else {
                        IconButton(onClick = {
                         navController.popBackStack()
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,contentDescription = "Go Back")
                        }
                    }
                },
                actions = {
                    if(currentRoute == Routes.NOTE_DETAIL){
                        IconButton(
                            onClick = {
                                navController.navigate(Routes.EDIT_NOTE)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Note"
                            )
                        }

                        IconButton(
                            onClick = shareNotes
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Note"
                            )
                        }
                    }
                }
            )
        },
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
                    selected = currentRoute == Routes.TASKS,
                    onClick = {
                        navController.navigate(Routes.TASKS){
                            popUpTo(navController.graph.startDestinationId){
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    icon = { Text("☑️") },
                    label = { Text("To-do") }
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
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable(route = Routes.NOTES) {
                NotesScreen(
                    notesViewModel = notesViewModel,
                    onAddNote = {
                        navController.navigate(Routes.CREATE_NOTE)
                    },
                    onEditNote = { note ->
                        selectedNote = note
                        navController.navigate(Routes.NOTE_DETAIL)
                    }

                )
            }

            composable(route = Routes.CREATE_NOTE) {
                CreateNoteScreen(
                    onSave = { title, content, formattedContent, imageUris, fileUris, audioFile, reminderTime, reminderRepeat, reminderEnabled ->

                        notesViewModel.addNote(
                            title, content, formattedContent = formattedContent,
                            onNoteSaved = { noteId ->
                                if (reminderEnabled) {
                                    notesViewModel.saveNoteReminder(
                                        noteId = noteId,
                                        title = title,
                                        reminderTime = reminderTime,
                                        reminderRepeat = reminderRepeat,
                                        isEnabled = reminderEnabled
                                    )
                                }

                                notesViewModel.saveImageAttachments(
                                    noteId = noteId,
                                    imageUris = imageUris
                                )
                                notesViewModel.saveFileAttachments(
                                    noteId = noteId,
                                    fileUris = fileUris
                                )
                                audioFile?.let { file ->
                                    notesViewModel.saveVoiceRecording(
                                        noteId = noteId,
                                        recordedFile = file
                                    )
                                }
                            },
                            onComplete = {
                                notesViewModel.deleteDraft("new_note")
                                navController.popBackStack()
                            })
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onAutoSave = { title, content, formattedContent, isMarkdown ->
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
                    },
                    onStartVoiceRecording = {
                        notesViewModel.startVoiceRecording()
                    },
                    onStopVoiceRecording = {
                        notesViewModel.stopVoiceRecordingForNewNote()
                    },
                    onCancelVoiceRecording = {
                        notesViewModel.cancelVoiceRecording()
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

            composable(Routes.EDIT_NOTE) {
                val note = selectedNote
                if (note != null) {
                    EditNoteScreen(
                        noteId = note.id,
                        initialTitle = note.title,
                        initialContent = note.preview,
                        initialFormattedContent = note.formattedContent,
                        initialReminderTime = note.reminderTime,
                        initialReminderRepeat = note.reminderRepeat,
                        initialReminderEnabled = note.isReminderEnabled,
                        onSave = { title, content, formattedContent, imageUris, fileUris, reminderTime, reminderRepeat, reminderEnabled ->
                            notesViewModel.saveNoteReminder(
                                noteId = note.id.toLong(),
                                title = title,
                                reminderTime = reminderTime,
                                reminderRepeat = reminderRepeat,
                                isEnabled = reminderEnabled
                            )
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
                        onAutoSave = { title, content, formattedContent, onComplete ->
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
                        onAutoSaveDraft = { title, content, formattedContent, isMarkdown ->
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
                        },
                        onStartVoiceRecording = {
                            notesViewModel.startVoiceRecording()
                        },
                        onStopVoiceRecording = {
                            notesViewModel.stopVoiceRecording(note.id.toLong())
                        },
                        onCancelVoiceRecording = {
                            notesViewModel.cancelVoiceRecording()
                        }
                    )
                }
            }

            composable(Routes.NOTE_DETAIL) {
                val notes by notesViewModel.notesFlow.collectAsState()
                val note = notes.find { it.id == selectedNote?.id } ?: selectedNote
                if (note != null) {
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
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.preview}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share note"))

                        },
                        onEdit = {
                            navController.navigate(Routes.EDIT_NOTE)
                        }
                    )
                }
            }

            composable(Routes.TASKS) {
                TaskListScreen(
                    onAddTask = {
                        selectedTask = null
                        navController.navigate(Routes.CREATE_TASK)
                    },
                    onEditTask = { task ->
                        selectedTask = task
                        navController.navigate(Routes.EDIT_TASK)
                    }
                )
            }

            composable(Routes.CREATE_TASK) {
                TaskEditorScreen(
                    task = null,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.EDIT_TASK) {
                TaskEditorScreen(
                    task = selectedTask,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.PRIVATE) {
                PrivateNotesScreen(
                    notesViewModel = notesViewModel,
                    onNoteClick = { note ->
                        selectedNote = note
                        navController.navigate(Routes.NOTE_DETAIL)
                    }
                )
            }
            composable(Routes.SETTINGS) {

                SettingsScreen(
                    onThemeChange = onThemeChange,
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

            composable(Routes.PRIVACY_SECURITY) {
                PrivacySecurityScreen(
                    onPinClick = {
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

            composable(Routes.APP_LOCK) {
                AppLockScreen()
            }
            composable(Routes.APP_INFO) {
                AppInfoScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.TAG_MANAGEMENT) {
                TagManagementScreen(onBack = {
                    navController.popBackStack()
                })
            }
        }
    }
    }
}