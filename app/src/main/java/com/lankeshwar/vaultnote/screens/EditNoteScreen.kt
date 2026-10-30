package com.lankeshwar.vaultnote.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.Utils.FileUtils.getDisplayFileName
import com.lankeshwar.vaultnote.Utils.NoteEditorUtils
import com.lankeshwar.vaultnote.Utils.NoteEditorUtils.insertBullet
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.data.db.NoteDraftEntity
import com.lankeshwar.vaultnote.viewmodels.NoteAttachmentViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.lankeshwar.vaultnote.Utils.AudioPlayerUtils
import com.lankeshwar.vaultnote.Utils.FileUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.contracts.contract

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun EditNoteScreen(
    noteId: Int,
    initialTitle: String,
    initialContent: String,
    initialFormattedContent: String,
    initialReminderTime: Long?,
    initialReminderRepeat: String,
    initialReminderEnabled: Boolean,
    onSave: (String, String, String, List<Uri>, List<Uri>,Long?, String, Boolean) -> Unit,
    onAutoSave: (String, String, String, () -> Unit) -> Unit,
    onCancel: () -> Unit,
    onAutoSaveDraft: (String, String, String, Boolean) -> Unit,
    onClearDraft: (String) -> Unit,
    onLoadDraft: suspend (String) -> NoteDraftEntity?,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
) {
    val context = LocalContext.current

    var reminderTime by remember(noteId, initialReminderTime) {
        mutableStateOf(initialReminderTime)
    }

    var reminderRepeat by remember(noteId, initialReminderRepeat) {
        mutableStateOf(initialReminderRepeat)
    }

    var reminderEnabled by remember(noteId, initialReminderEnabled) {
        mutableStateOf(initialReminderEnabled)
    }

    val reminderDateText = reminderTime?.let { time ->
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        ).format(time)
    } ?: "No reminder set"

    // Note content
    var title by remember(noteId, initialTitle) {
        mutableStateOf(initialTitle)
    }

    var content by remember(noteId, initialFormattedContent, initialContent) {
        mutableStateOf(
            if (initialFormattedContent.isNotBlank()) {
                RichTextUtils.fromHtml(initialFormattedContent)
            } else {
                TextFieldValue(text = initialContent)
            }
        )
    }

    var markdownMode by remember(noteId) { mutableStateOf(false) }
    var isDraftLoaded by remember(noteId) { mutableStateOf(false) }

    // Autosave status
    var autoSaveStatus by remember { mutableStateOf("") }

    var lastSavedTitle by remember(noteId, initialTitle) {
        mutableStateOf(initialTitle)
    }

    var lastSavedContent by remember(noteId, initialContent) {
        mutableStateOf(initialContent)
    }

    var lastSavedHtml by remember(noteId, initialFormattedContent) {
        mutableStateOf(initialFormattedContent)
    }

    // Newly selected attachments
    var selectedImageUris by remember(noteId) {
        mutableStateOf<List<Uri>>(emptyList())
    }

    var selectedFileUris by remember(noteId) {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImageUris = selectedImageUris + uris
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFileUris = selectedFileUris + uris
    }

    // Existing attachments from Room
    val attachmentViewModel: NoteAttachmentViewModel = hiltViewModel()

    val existingAttachments by attachmentViewModel
        .getAttachmentsForNote(noteId)
        .collectAsState(initial = emptyList())

    // Voice recording
    var isRecording by remember { mutableStateOf(false) }
    var recordingError by remember { mutableStateOf<String?>(null) }

    var isListening by remember { mutableStateOf(false) }
    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            isListening = false

            if(result.resultCode == android.app.Activity.RESULT_OK){
                val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()

                if(!spokenText.isNullOrBlank()){
                    val currentText = content.text
                    val start = minOf(content.selection.start,content.selection.end)
                    val end = maxOf(content.selection.start,content.selection.end)

                    val updatedText = buildString {
                        append(currentText.subSequence(0,start))
                        append(spokenText)
                        append(currentText.substring(end))
                    }
                    val cursorPosition = start + spokenText.length
                    content = TextFieldValue(text=updatedText,
                        selection = TextRange(cursorPosition))
                }

            }
        }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your note"
                )
            }

            isListening = true
            speechRecognizerLauncher.launch(intent)
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // Audio playback
    val audioPlayer = remember { AudioPlayerUtils() }

    var playingFilePath by remember {
        mutableStateOf<String?>(null)
    }

    var isAudioPlaying by remember {
        mutableStateOf(false)
    }

    fun playAudio(filePath: String) {
        playingFilePath = filePath

        audioPlayer.play(
            filePath = filePath,
            onPlayingChanged = { playing ->
                isAudioPlaying = playing
                playingFilePath = if (playing) filePath else null
            },
            onError = {
                playingFilePath = null
                isAudioPlaying = false

                Toast.makeText(
                    context,
                    "Unable to play this recording",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    fun stopAudioPlayback() {
        audioPlayer.stop { playing ->
            isAudioPlaying = playing
        }

        playingFilePath = null
    }

    DisposableEffect(audioPlayer) {
        onDispose {
            audioPlayer.release()
        }
    }

    // Load the saved draft
    LaunchedEffect(noteId) {
        val draftId = "edit_note_$noteId"
        val draft = onLoadDraft(draftId)

        if (draft != null) {
            title = draft.title
            markdownMode = draft.isMarkdown

            content = if (draft.isMarkdown) {
                TextFieldValue(draft.content)
            } else if (draft.formattedContent.isNotBlank()) {
                RichTextUtils.fromHtml(draft.formattedContent)
            } else {
                TextFieldValue(draft.content)
            }

            lastSavedTitle = draft.title
            lastSavedContent = draft.content
            lastSavedHtml = if (draft.formattedContent.isNotBlank()) {
                draft.formattedContent
            } else {
                RichTextUtils.toHtml(content.annotatedString)
            }
        }

        isDraftLoaded = true
    }

    // Save draft while editing
    LaunchedEffect(title, content, markdownMode, isDraftLoaded) {
        if (!isDraftLoaded) return@LaunchedEffect

        val draftId = "edit_note_$noteId"

        if (title.isBlank() && content.text.isBlank()) {
            onClearDraft(draftId)
            return@LaunchedEffect
        }

        delay(500L.milliseconds)

        val savedHtml = if (markdownMode) {
            RichTextUtils.markdownToHtml(content.text)
        } else {
            RichTextUtils.toHtml(content.annotatedString)
        }

        onAutoSaveDraft(
            title,
            content.text,
            savedHtml,
            markdownMode
        )
    }

    // Automatically save note changes
    LaunchedEffect(title, content, markdownMode, isDraftLoaded) {
        if (!isDraftLoaded) return@LaunchedEffect

        if (title.isBlank() && content.text.isBlank()) {
            autoSaveStatus = ""
            return@LaunchedEffect
        }

        val savedHtml = if (markdownMode) {
            RichTextUtils.markdownToHtml(content.text)
        } else {
            RichTextUtils.toHtml(content.annotatedString)
        }

        if (
            title == lastSavedTitle &&
            content.text == lastSavedContent &&
            savedHtml == lastSavedHtml
        ) {
            return@LaunchedEffect
        }

        autoSaveStatus = "Unsaved changes"
        delay(1000L)

        autoSaveStatus = "Saving…"

        onAutoSave(title, content.text, savedHtml) {
            lastSavedTitle = title
            lastSavedContent = content.text
            lastSavedHtml = savedHtml
            autoSaveStatus = "Saved"
        }
    }

    // Apply formatting to the selected text.
    // Existing styles are preserved when another style is applied.
    fun applyStyle(style: SpanStyle) {
        val start = minOf(content.selection.start, content.selection.end)
        val end = maxOf(content.selection.start, content.selection.end)

        if (start >= end) return

        val builder = AnnotatedString.Builder(content.annotatedString)
        builder.addStyle(style, start, end)

        content = content.copy(
            annotatedString = builder.toAnnotatedString(),
            selection = TextRange(start, end)
        )
    }

    fun applyHeading(size: Int) {
        applyStyle(
            SpanStyle(
                fontSize = size.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen heading
        Text(
            text = "Edit Note",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = autoSaveStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Reminder",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = reminderDateText,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val calendar = Calendar.getInstance().apply {
                        reminderTime?.let { timeInMillis = it }
                    }

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, day)

                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)

                                    reminderTime = calendar.timeInMillis
                                    reminderEnabled = true
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                            ).show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("Set Date & Time")
            }

            TextButton (
                onClick = {
                    reminderTime = null
                    reminderEnabled = false
                    reminderRepeat = "NONE"
                }
            ) {
                Text("Clear")
            }
        }

        Text(
            text = "Repeat",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "NONE" to "Once",
                "DAILY" to "Daily",
                "WEEKLY" to "Weekly",
                "MONTHLY" to "Monthly"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = reminderRepeat == value,
                    onClick = {
                        reminderRepeat = value
                    },
                    label = {
                        Text(label)
                    }
                )
            }
        }

        // Editable note title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true
        )

        // Formatting and attachment toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speech to Text
            OutlinedButton(
                onClick = {
                    val permissionGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                    if(permissionGranted){
                        val intent = Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                        ).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Speak your note"
                            )
                        }

                        isListening = true
                        speechRecognizerLauncher.launch(intent)
                    } else {
                        microphonePermissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speech to text"
                )
                Text(if (isListening) "Listening…" else "Speak")
            }


            OutlinedButton(
                onClick = {
                    applyStyle(SpanStyle(fontWeight = FontWeight.Bold))
                },
                enabled = !content.selection.collapsed
            ) {
                Text("B", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    applyStyle(SpanStyle(fontStyle = FontStyle.Italic))
                },
                enabled = !content.selection.collapsed
            ) {
                Text("I", fontStyle = FontStyle.Italic)
            }

            OutlinedButton(
                onClick = {
                    applyStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline)
                    )
                },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    "U",
                    textDecoration = TextDecoration.Underline
                )
            }

            OutlinedButton(
                onClick = {
                    applyStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough)
                    )
                },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    "S",
                    textDecoration = TextDecoration.LineThrough
                )
            }

            OutlinedButton(
                onClick = { applyHeading(28) },
                enabled = !content.selection.collapsed
            ) {
                Text("H1")
            }

            OutlinedButton(
                onClick = { applyHeading(24) },
                enabled = !content.selection.collapsed
            ) {
                Text("H2")
            }

            OutlinedButton(
                onClick = { applyHeading(20) },
                enabled = !content.selection.collapsed
            ) {
                Text("H3")
            }

            OutlinedButton(
                onClick = {
                    content = NoteEditorUtils.insertBullet(content)
                }
            ) {
                Text("• List")
            }

            OutlinedButton(
                onClick = {
                    content = NoteEditorUtils.insertChecklistItem(content)
                }
            ) {
                Text("☐ Checklist")
            }

            OutlinedButton(
                onClick = {
                    content = NoteEditorUtils.insertNumberedList(content)
                }
            ) {
                Text("1. List")
            }

            OutlinedButton(
                onClick = {
                    markdownMode = !markdownMode
                }
            ) {
                Text(
                    if (markdownMode) "Markdown: ON" else "Markdown"
                )
            }

            OutlinedButton(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                }
            ) {
                Text("Add Images")
            }

            OutlinedButton(
                onClick = {
                    documentPickerLauncher.launch("*/*")
                }
            ) {
                Text("Attach Files")
            }

            OutlinedButton(
                onClick = {
                    recordingError = null

                    val permissionGranted =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                    if (permissionGranted) {
                        try {
                            onStartVoiceRecording()
                            isRecording = true
                        } catch (e: Exception) {
                            recordingError =
                                e.message ?: "Unable to start recording"
                        }
                    } else {
                        microphonePermissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                },
                enabled = !isRecording
            ) {
                Text("Start Recording")
            }

            OutlinedButton(
                onClick = {
                    try {
                        onStopVoiceRecording()
                        isRecording = false
                        recordingError = null
                    } catch (e: Exception) {
                        recordingError =
                            e.message ?: "Unable to stop recording"
                    }
                },
                enabled = isRecording
            ) {
                Text("Stop Recording")
            }

            OutlinedButton(
                onClick = {
                    onCancelVoiceRecording()
                    isRecording = false
                    recordingError = null
                },
                enabled = isRecording
            ) {
                Text("Cancel Recording")
            }
        }

        recordingError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Note content editor
        BasicTextField(
            value = content,
            onValueChange = { newValue ->
                content = NoteEditorUtils.handleNumberedListInput(
                    previous = content,
                    current = newValue
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (content.text.isEmpty()) {
                        Text(
                            text = "Write your note",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    innerTextField()
                }
            }
        )

        // Newly selected images and files.
        // These are previews of selections, not yet saved attachments.
        if (selectedImageUris.isNotEmpty() || selectedFileUris.isNotEmpty()) {
            Text(
                text = "New Attachments",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedImageUris.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Selected image"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = FileUtils.getDisplayFileName(context, uri),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                selectedFileUris.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Selected file"
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = FileUtils.getDisplayFileName(context, uri),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Attachments already saved for this note.
        if (existingAttachments.isNotEmpty()) {
            HorizontalDivider()

            Text(
                text = "Existing Attachments",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                existingAttachments.forEach { attachment ->
                    when {
                        attachment.fileType.startsWith("image/") -> {
                            val bitmap = remember(attachment.filePath) {
                                runCatching {
                                    BitmapFactory.decodeFile(attachment.filePath)
                                }.getOrNull()
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = attachment.fileName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp),
                                    contentScale = ContentScale.Fit
                                )

                                Text(
                                    text = attachment.fileName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "Unable to display ${attachment.fileName}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        attachment.fileType.startsWith("audio/") -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Audio attachment"
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = attachment.fileName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = if (
                                            playingFilePath == attachment.filePath &&
                                            isAudioPlaying
                                        ) {
                                            "Playing…"
                                        } else {
                                            "Audio recording"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (playingFilePath == attachment.filePath) {
                                            if (isAudioPlaying) {
                                                audioPlayer.pause { playing ->
                                                    isAudioPlaying = playing
                                                }
                                            } else {
                                                audioPlayer.resume { playing ->
                                                    isAudioPlaying = playing
                                                }
                                            }
                                        } else {
                                            playAudio(attachment.filePath)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (
                                            playingFilePath == attachment.filePath &&
                                            isAudioPlaying
                                        ) {
                                            Icons.Default.Pause
                                        } else {
                                            Icons.Default.PlayArrow
                                        },
                                        contentDescription = if (
                                            playingFilePath == attachment.filePath &&
                                            isAudioPlaying
                                        ) {
                                            "Pause"
                                        } else {
                                            "Play"
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = { stopAudioPlayback() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop playback"
                                    )
                                }
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val file = File(attachment.filePath)

                                            if (!file.exists()) {
                                                Toast.makeText(
                                                    context,
                                                    "File not found",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val fileUri =
                                                    FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(fileUri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }

                                                try {
                                                    context.startActivity(
                                                        Intent.createChooser(
                                                            intent,
                                                            "Open PDF with"
                                                        )
                                                    )
                                                } catch (e: android.content.ActivityNotFoundException) {
                                                    Toast.makeText(
                                                        context,
                                                        "No PDF viewer can open this file",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }catch (e: Exception){
                                            Toast.makeText(context, "No app available to open this file",Toast.LENGTH_SHORT).show()
                                        }

                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Open attachment"
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = attachment.fileName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = attachment.fileType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // Always visible at the end of the scrollable content.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val savedHtml = if (markdownMode) {
                        RichTextUtils.markdownToHtml(content.text)
                    } else {
                        RichTextUtils.toHtml(content.annotatedString)
                    }

                    onSave(
                        title,
                        content.text,
                        savedHtml,
                        selectedImageUris,
                        selectedFileUris,
                        reminderTime,
                        reminderRepeat,
                        reminderEnabled
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
