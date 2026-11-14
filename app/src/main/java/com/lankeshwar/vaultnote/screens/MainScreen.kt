package com.lankeshwar.vaultnote.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lankeshwar.vaultnote.Utils.AppLockPreferences
import com.lankeshwar.vaultnote.Utils.Routes
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.data.model.JournalTemplate
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppInfoScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.AppLockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.ChangePinScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.CloudSyncScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.FingerprintUnlockScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.PrivacySecurityScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.RecentlyDeletedScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.TagManagementScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.AutomaticBackupSettingsScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.BackupHistoryScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.BackupRestoreMenuScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.CreateBackupScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.RestoreBackupScreen
import com.lankeshwar.vaultnote.screens.SettingSecurity.backup.TrashRetentionSettings
import com.lankeshwar.vaultnote.screens.journal.JournalEditorScreen
import com.lankeshwar.vaultnote.screens.journal.JournalScreen
import com.lankeshwar.vaultnote.screens.journal.JournalTemplateScreen
import com.lankeshwar.vaultnote.screens.onboard.LoginScreen
import com.lankeshwar.vaultnote.screens.onboard.RegisterScreen
import com.lankeshwar.vaultnote.screens.tasks.TaskEditorScreen
import com.lankeshwar.vaultnote.screens.tasks.TaskListScreen
import com.lankeshwar.vaultnote.theme.AppTheme
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import com.lankeshwar.vaultnote.viewmodels.TaskViewModel
import com.lankeshwar.vaultnote.viewmodels.backup.AutomaticBackupSettingsViewModel
import com.lankeshwar.vaultnote.viewmodels.backup.TrashRetentionSettingsViewModel
import kotlinx.coroutines.launch
import okhttp3.Route


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun MainScreen(selectTheme: AppTheme? = null,
               onThemeChange: (AppTheme) -> Unit = {},
               notificationNoteId:Int? = null,
               onNotificationHandled:()-> Unit = {},
               widgetTaskId:Int? = null,
               onWidgetTaskHandled:()->Unit = {},
               openCreateNote: Boolean = false,
               onCreateNoteHandled: () -> Unit = {}) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val taskViewModel: TaskViewModel = hiltViewModel()
    val tasks by taskViewModel.tasks.collectAsState()
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()

    val notes by notesViewModel.notesFlow.collectAsState()
    var showDeleteNoteDialog by remember { mutableStateOf(false) }
    var handleNotificationNoteId by remember { mutableStateOf<Int?>(null) }
    val currentRoute = backstackEntry?.destination?.route
    var selectedNote by remember {
        mutableStateOf<Note?>(null)
    }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    var widgetTaskId by remember { mutableStateOf<Int?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedJournalEntry by remember {mutableStateOf<JournalEntity?>(null)}
    var selectedJournalDate by remember {mutableStateOf("")}
    var selectedJournalTemplate by remember {
        mutableStateOf<JournalTemplate?>(null)
    }
    var openedTempleteFromEditor by remember{mutableStateOf(false)}

    val isSecureScreen = currentRoute == Routes.PRIVATE || (currentRoute == Routes.NOTE_DETAIL && selectedNote?.isPrivate == true)

    LaunchedEffect(currentRoute,selectedNote?.isPrivate) {
        val isInsidePrivateSection = currentRoute == Routes.PRIVATE ||
                (
                        (currentRoute == Routes.NOTE_DETAIL || currentRoute == Routes.EDIT_NOTE) &&
                selectedNote?.isPrivate == true)
        if(!isInsidePrivateSection){
            notesViewModel.lockPrivateNotes()
        }
    }

    DisposableEffect(activity, currentRoute, selectedNote?.isPrivate) {

        val observer = object : androidx.lifecycle.LifecycleEventObserver {

            override fun onStateChanged(
                source: androidx.lifecycle.LifecycleOwner,
                event: Lifecycle.Event
            ) {
                if (event == Lifecycle.Event.ON_STOP) {

                    val isInsidePrivateSection =
                        currentRoute == Routes.PRIVATE ||
                                (
                                        (currentRoute == Routes.NOTE_DETAIL ||
                                                currentRoute == Routes.EDIT_NOTE) &&
                                                selectedNote?.isPrivate == true
                                )

                    if (isInsidePrivateSection &&
                        AppLockPreferences.isAppLockEnabled(context)
                    ) {
                       notesViewModel.lockPrivateNotes()
                    }
                }
            }
        }

        val lifecycleOwner = activity as? LifecycleOwner
        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    SideEffect {
        if(isSecureScreen){
            activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

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

    LaunchedEffect(widgetTaskId,tasks) {
        val taskId = widgetTaskId
        if(taskId != null){
            val targetTask = tasks.find { it.id == taskId }

            if(targetTask!= null){
                selectedTask = targetTask
                navController.navigate(Routes.EDIT_TASK){
                    launchSingleTop = true
                }
                onWidgetTaskHandled()
            }
        }
    }

    LaunchedEffect(openCreateNote) {
        if (openCreateNote) {
            navController.navigate(Routes.CREATE_NOTE) {
                launchSingleTop = true
            }
            onCreateNoteHandled()
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

                NavigationDrawerItem(
                    label = { Text("Daily Journal") },
                    selected = currentRoute == Routes.JOURNAL,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Routes.JOURNAL) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = { Text("📔") },
                )
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
            Routes.JOURNAL -> "Daily Journal"
            Routes.BACKUP_RESTORE -> "Backup & Restore"
            Routes.CREATE_BACKUP -> "Create Backup"
            Routes.BACKUP_HISTORY -> "Backup History"
            Routes.RESTORE_BACKUP -> "Restore Backup"
            Routes.TRASH_RETENTION -> "Trash Retention Settings"
            Routes.AUTOMATIC_BACKUP_SETTINGS -> "Automatic Backups"
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
                            onClick = {showDeleteNoteDialog = true}
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
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
                    } else if (currentRoute == Routes.EDIT_NOTE){
                        IconButton(
                            onClick = {showDeleteNoteDialog = true}
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
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
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
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
                        isPrivate = note.isPrivate,
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
                        notesViewModel = notesViewModel,
                        content = note.preview,
                        formattedContent = note.formattedContent,
                        date = note.date,
                        isPrivate = note.isPrivate,
                        onBack = {
                            navController.popBackStack()
                        },
                        onRelatedNoteClick = { relatedNote ->
                            selectedNote = relatedNote

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
                        },
                        onDelete = {
                            notesViewModel.deleteNotes(note)
                            navController.popBackStack()
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
                    },
                    onBackupClick = {
                        navController.navigate(Routes.BACKUP_RESTORE)
                    },
                    onCloudSyncClick = {
                        navController.navigate(Routes.CLOUD_SYNC)
                    }
                )
            }

            composable(Routes.BACKUP_RESTORE){
                BackupRestoreMenuScreen(
                    onCreateBackupClick = {
                        navController.navigate(Routes.CREATE_BACKUP)
                    },
                    onBackupHistoryClick = {
                        navController.navigate(Routes.BACKUP_HISTORY)
                    },
                    onRestoreBackupClick = {
                        navController.navigate(Routes.RESTORE_BACKUP)
                    },
                    onAutomaticBackupClick = {
                        navController.navigate(Routes.AUTOMATIC_BACKUP_SETTINGS)
                    },
                    onTrashRetentionClick = {
                        navController.navigate(Routes.TRASH_RETENTION)
                    }
                )
            }

            composable(Routes.CREATE_BACKUP){
                CreateBackupScreen()
            }

            composable(Routes.BACKUP_HISTORY){
                BackupHistoryScreen()
            }

            composable(Routes.RESTORE_BACKUP){
                RestoreBackupScreen()
            }

            composable(Routes.TRASH_RETENTION){
                val trashRetentionViewModel: TrashRetentionSettingsViewModel = hiltViewModel()
                val selectedRetentionDays by trashRetentionViewModel.retentionDays.collectAsState()
                TrashRetentionSettings(
                    selectedRetentionDays = selectedRetentionDays,
                    onRetentionDaysChange = {days ->
                        trashRetentionViewModel.updateRetentionDays(days)
                    },
                )
            }

            composable(Routes.AUTOMATIC_BACKUP_SETTINGS) {
                val backupSettingsViewModel: AutomaticBackupSettingsViewModel = hiltViewModel()
                val backupState by backupSettingsViewModel.uiState.collectAsState()

                AutomaticBackupSettingsScreen(
                    isEnabled = backupState.isEnabled,
                    frequency = backupState.frequency,
                    isPasswordConfigured = backupState.isPasswordConfigured,
                    lastBackupText = backupState.lastBackupText,
                    onEnabledChange = { enabled ->
                        backupSettingsViewModel.updateEnabled(enabled)
                    },
                    onPasswordSave = { password ->
                        backupSettingsViewModel.savePassword(password)
                    },
                    onFrequencyChange = {frequency ->
                        backupSettingsViewModel.updateFrequency(frequency)
                    }
                )
            }

            composable(Routes.CLOUD_SYNC){
                CloudSyncScreen(
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN)
                    },
                    onRegisterClick = {
                        navController.navigate(Routes.REGISTER)
                    },
                )
            }

            composable(Routes.LOGIN){
                LoginScreen(
                    onRegisterClick = {
                        navController.navigate(Routes.REGISTER)
                    },
                    onLoginSuccess = {
                        navController.popBackStack(
                            Routes.CLOUD_SYNC,
                            inclusive = false
                        )
                    }
                )
            }

            composable(Routes.REGISTER){
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.popBackStack(Routes.CLOUD_SYNC,inclusive = false)
                    },
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.CLOUD_SYNC)
                        }
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

            composable(Routes.JOURNAL){
                JournalScreen(
                    onAddEntry = { date ->
                        selectedJournalDate = date
                        selectedJournalEntry = null
                        selectedJournalTemplate = null
                        navController.navigate(Routes.JOURNAL_EDITOR)
                    },
                    onEditEntry = { journalEntity ->
                        selectedJournalEntry = journalEntity
                        navController.navigate(Routes.JOURNAL_EDITOR)
                    },
                    onChooseTemplate = { date ->
                        selectedJournalDate = date
                        selectedJournalEntry = null
                        selectedJournalTemplate = null
                        navController.navigate(Routes.JOURNAL_TEMPLATES)
                    }
                )
            }

            composable(Routes.JOURNAL_EDITOR) {
                val selectedDate = selectedJournalEntry?.date
                    ?: selectedJournalDate

                JournalEditorScreen(
                    date = selectedDate,
                    existingEntry = selectedJournalEntry,
                    templateToInsert = selectedJournalTemplate,
                    onBack = {
                        navController.popBackStack()
                    },
                    onChooseTemplate = {
                        openedTempleteFromEditor = true
                        navController.navigate(Routes.JOURNAL_TEMPLATES)
                    },
                    onTemplateConsumed = {
                        selectedJournalTemplate = null
                    }
                )
            }

            composable( Routes.JOURNAL_TEMPLATES){
                JournalTemplateScreen(
                    onTemplateSelected = { template ->
                        selectedJournalTemplate = template
                        if(openedTempleteFromEditor){
                            openedTempleteFromEditor = false
                            navController.popBackStack()
                        } else {
                            navController.navigate(Routes.JOURNAL_EDITOR)
                        }
                    },
                    onStartBlank = {
                        if(openedTempleteFromEditor){
                            openedTempleteFromEditor = false
                            selectedJournalTemplate = null
                            navController.popBackStack()
                        }else {
                            selectedJournalTemplate = null
                            selectedJournalEntry = null
                            navController.navigate(Routes.JOURNAL_EDITOR)
                        }
                    },

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        if(showDeleteNoteDialog){
            AlertDialog(
                onDismissRequest = {
                    showDeleteNoteDialog = false
                },
                title = {            Text("Delete note?") },
                text = {            Text("This note will be moved to Trash.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteNoteDialog = false
                            selectedNote?.let { note ->
                                notesViewModel.deleteNotes(note)
                            }
                            navController.navigate(Routes.NOTES){
                                popUpTo(Routes.NOTES){
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {showDeleteNoteDialog = false}) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}