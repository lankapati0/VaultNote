package com.lankeshwar.vaultnote.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lankeshwar.vaultnote.Utils.NoteEditorUtils
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.data.db.NoteDraftEntity
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.ui.text.substring
import androidx.core.content.ContextCompat
import com.lankeshwar.vaultnote.Utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

@Composable
fun CreateNoteScreen(
    onSave: (String, String, String, List<Uri>, List<Uri>,File?,Long?,String, Boolean)-> Unit,
    onCancel: () -> Unit,
    onAutoSave: (String, String, String, Boolean) -> Unit,
    onLoadDraft: suspend () -> NoteDraftEntity?,
    onClearDraft: () -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopVoiceRecording: () -> File?,
    onCancelVoiceRecording: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var markdownMode by remember { mutableStateOf(false) }

    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var reminderRepeat by remember { mutableStateOf("NONE") }
    var reminderEnabled by remember { mutableStateOf(false) }

    val reminderDateText = reminderTime?.let {time ->
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(time)
    } ?: "No reminder set"

    var isDraftLoaded by remember { mutableStateOf(false) }

    var selectedImageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    // Autosave status
    var autoSaveStatus by remember {
        mutableStateOf("")
    }

    var lastSavedTitle by remember {
        mutableStateOf("")
    }

    var lastSavedContent by remember {
        mutableStateOf("")
    }

    var lastSavedHtml by remember {
        mutableStateOf("")
    }

    var selectedFileUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImageUris = selectedImageUris + uris
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedFileUris = selectedFileUris + uris
    }

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

    var isRecording by remember {mutableStateOf(false)}
    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var recordingError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val draft = onLoadDraft()

        if(draft != null){
            title = draft.title
            markdownMode = draft.isMarkdown
            content = if(draft.isMarkdown){
                TextFieldValue(draft.content)
            } else if (draft.formattedContent.isNotBlank()){
                RichTextUtils.fromHtml(draft.formattedContent)
            } else {
                TextFieldValue(draft.content)
            }
        }
        isDraftLoaded = true
    }

    LaunchedEffect(title,content,markdownMode,isDraftLoaded) {
        if(!isDraftLoaded) return@LaunchedEffect

        if(title.isBlank() && content.text.isBlank()){
            autoSaveStatus = ""
            return@LaunchedEffect
        }
        delay(1000.milliseconds)
        val savedHtml = if(markdownMode){
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
        delay(1000L.milliseconds)
        onAutoSave(title, content.text, savedHtml, markdownMode)

        lastSavedTitle = title
        lastSavedContent = content.text
        lastSavedHtml = savedHtml
        autoSaveStatus = "Saving…"

    }

    fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(intent, "Open PDF with")
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open PDF. Please install a PDF viewer.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun applyStyle(style: SpanStyle) {
        val start = minOf(content.selection.start, content.selection.end)
        val end = maxOf(content.selection.start, content.selection.end)

        if (start >= end) return

        val selectedText = content.annotatedString
        val selectedStyles = selectedText.spanStyles.filter {
            it.start < end && it.end > start
        }

        val isUnderline = style.textDecoration == TextDecoration.Underline
        val isStrike = style.textDecoration == TextDecoration.LineThrough
        val isBold = style.fontWeight == FontWeight.Bold
        val isItalic = style.fontStyle == FontStyle.Italic

        val alreadyApplied = when {
            isUnderline -> selectedStyles.all {
                it.item.textDecoration?.contains(TextDecoration.Underline) == true
            }
            isStrike -> selectedStyles.all {
                it.item.textDecoration?.contains(TextDecoration.LineThrough) == true
            }
            isBold -> selectedStyles.all {
                it.item.fontWeight == FontWeight.Bold
            }
            isItalic -> selectedStyles.all {
                it.item.fontStyle == FontStyle.Italic
            }
            else -> false
        }

        val builder = AnnotatedString.Builder(selectedText)

        // Remove only the selected property, or add the requested style.
        if (alreadyApplied) {
            // Removing a single property safely requires rebuilding the selected
            // ranges; don't replace unrelated styles.
            val plainBuilder = AnnotatedString.Builder()
            plainBuilder.append(selectedText.subSequence(0, start))
            plainBuilder.append(selectedText.subSequence(start, end))
            plainBuilder.append(selectedText.subSequence(end, selectedText.length))

            content = content.copy(
                annotatedString = plainBuilder.toAnnotatedString(),
                selection = TextRange(start, end)
            )
            return
        }

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
        modifier = Modifier.fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Note", style = MaterialTheme.typography.headlineMedium
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
            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance()

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selectedCalendar = Calendar.getInstance().apply {
                                timeInMillis = reminderTime ?: System.currentTimeMillis()
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }

                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                                    selectedCalendar.set(Calendar.MINUTE, minute)
                                    selectedCalendar.set(Calendar.SECOND, 0)

                                    reminderTime = selectedCalendar.timeInMillis
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

            OutlinedButton(
                onClick = {
                    reminderTime = null
                    reminderEnabled = false
                    reminderRepeat = "NONE"
                }
            ) {
                Text("Clear")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
            listOf("NONE","DAILY","WEEKLY","MONTHLY").forEach { repeat ->
                FilterChip(
                    selected = reminderRepeat == repeat,
                    onClick = {reminderRepeat = repeat},
                    label = {
                        Text(
                            when(repeat){
                                "NONE" -> "ONCE"
                                "DAILY" -> "Daily"
                                "WEEKLY" -> "Weekly"
                                else -> "Monthly"
                            }
                        )
                    }
                )
            }
        }
        Text(
            text = autoSaveStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = title,
            onValueChange = {title = it},
            label = {Text("Note title")},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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

            // Bold Button
            OutlinedButton(
                onClick = { applyStyle(SpanStyle(fontWeight = FontWeight.Bold))},
                enabled = !content.selection.collapsed
            ) {
                Text(
                    text = "B",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { applyStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    text = "I",
                    fontStyle = FontStyle.Italic
                )
            }

            OutlinedButton(
                onClick = { applyStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    text = "U",
                    textDecoration = TextDecoration.Underline
                )
            }

            // Line-Through Button
            OutlinedButton(
                onClick = { applyStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    text = "L",
                    textDecoration = TextDecoration.LineThrough
                )
            }

            // H1 Heading
            OutlinedButton(
                onClick = {applyHeading(28)},
                enabled = !content.selection.collapsed
            ) {
                Text("H1")
            }
            // H2 Heading
            OutlinedButton(
                onClick = {applyHeading(24)},
                enabled = !content.selection.collapsed
            ) {
                Text("H2")
            }
            // H3 Heading
            OutlinedButton(
                onClick = {applyHeading(20)},
                enabled = !content.selection.collapsed
            ) {
                Text("H3")
            }
            // List Button
            OutlinedButton(onClick = {
                content = NoteEditorUtils.insertBullet(content)
            }) {
                Text("• List")
            }
            // Checklist button
            OutlinedButton(onClick = {
                content = NoteEditorUtils.insertChecklistItem(content)
            }) {
                Text("☐ Checklist")
            }
            // List button
            OutlinedButton(
                onClick = {
                    content = NoteEditorUtils.insertNumberedList(content)
                }
            ) {
                Text("1. List")
            }
            // Markdown button
            OutlinedButton(
                onClick = { markdownMode = !markdownMode }
            ) {
                Text(if (markdownMode) "Markdown: ON" else "Markdown")
            }

            OutlinedButton(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                }
            ) {
                Text("🖼 Attach Images")
            }
            OutlinedButton(
                onClick = {
                    documentPickerLauncher.launch("*/*")
                }
            ) {
                Text("📎 Attach Files")
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
                        recordedAudioFile = onStopVoiceRecording()
                        isRecording = false
                        recordingError = if (recordedAudioFile == null) {
                            "Unable to save recording."
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        recordingError =
                            e.message ?: "Unable to stop recording"
                        isRecording = false
                    }
                },
                enabled = isRecording
            ) {
                Text("Stop Recording")
            }

            OutlinedButton(onClick = {
                try{
                    onCancelVoiceRecording()
                } finally {
                    isRecording = false
                    recordedAudioFile = null
                    recordingError = null
                }
            },
                enabled = isRecording || recordedAudioFile != null
            ) {
                Text("Cancel Recording")
            }

        }

        if(isRecording){
            Text("Recording in progress…")
        }

        recordedAudioFile?.let {
            Text("Recording ready to save with your note.")
        }

        recordingError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(selectedImageUris){uri ->
                val bitmap = remember(uri) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                }

                if(bitmap != null){
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected attachment",
                        modifier = Modifier.height(100.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

/*        if(selectedImageUris.isNotEmpty()){
            Text(text="Attached Files", style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedImageUris.forEach { uri ->
                    Text(
                        text = "📎 ${FileUtils.getDisplayFileName(context, uri)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }*/

        if (selectedFileUris.isNotEmpty()) {
            Text(
                text = "Attached Files",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedFileUris.forEach { uri ->
                    val fileName = FileUtils.getDisplayFileName(context, uri)
                    val mimeType = context.contentResolver.getType(uri)

                    Text(
                        text = "📎 $fileName",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (
                                    mimeType == "application/pdf" ||
                                    fileName.endsWith(".pdf", ignoreCase = true)
                                ) {
                                    openPdf(uri)
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        BasicTextField(
            value = content,
            onValueChange = { newValue ->
                content = NoteEditorUtils.handleNumberedListInput(previous = content,
                    current = newValue)
            },
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextfield ->
                Box{
                    if(content.text.isEmpty()) {
                        Text(text="Write your note", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextfield()
                }
            }
        )


        Button(
            onClick = {
                val savedHtml = if(markdownMode){
                    RichTextUtils.markdownToHtml(content.text)
                } else {
                    RichTextUtils.toHtml(content.annotatedString)
                }
            onSave(title,content.text, savedHtml,selectedImageUris,selectedFileUris,recordedAudioFile,reminderTime,reminderRepeat,reminderEnabled)},
            enabled = title.isNotBlank() || content.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Note")
        }

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
