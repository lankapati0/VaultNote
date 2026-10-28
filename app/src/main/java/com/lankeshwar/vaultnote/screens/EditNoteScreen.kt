package com.lankeshwar.vaultnote.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.ui.graphics.asImageBitmap
@SuppressLint("QueryPermissionsNeeded")
@Composable
fun EditNoteScreen(
    noteId:Int,
    initialTitle: String,
    initialContent: String,
    initialFormattedContent: String,
    onSave: (String, String, String,List<Uri>,List<Uri>) -> Unit,
    onAutoSave: (String, String, String, () -> Unit) -> Unit,
    onCancel: () -> Unit,
    onAutoSaveDraft: (String, String, String, Boolean) -> Unit,
    onClearDraft: (String) -> Unit,
    onLoadDraft: suspend (String) -> NoteDraftEntity?
) {
    var title by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }
    var markdownMode by remember { mutableStateOf(false) }
    var isDraftLoaded by remember(noteId) { mutableStateOf(false) }
    var content by remember(initialFormattedContent, initialContent) {
        mutableStateOf(
            if (initialFormattedContent.isNotBlank()) {
                RichTextUtils.fromHtml(initialFormattedContent)
            } else {
                TextFieldValue(text = initialContent)
            }
        )
    }

    var autoSaveStatus by remember { mutableStateOf("") }
    var lastSavedTitle by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }

    var lastSavedContent by remember(initialContent) {
        mutableStateOf(initialContent)
    }

    var lastSavedHtml by remember(initialFormattedContent) {
        mutableStateOf(initialFormattedContent)
    }

    var selectedImageUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    var selectedFileUris by remember {
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
    val attachmentViewModel: NoteAttachmentViewModel = hiltViewModel()
    val context = LocalContext.current
    val existingAttachments by attachmentViewModel.getAttachmentsForNote(noteId)
        .collectAsState(initial = emptyList())

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
            lastSavedHtml = draft.formattedContent
        }
        isDraftLoaded = true
    }

    LaunchedEffect(title, content, markdownMode, isDraftLoaded) {
        if (!isDraftLoaded) return@LaunchedEffect

        if (title.isBlank() && content.text.isBlank()) {
            onClearDraft("edit_note_$noteId")
            return@LaunchedEffect
        }
        delay(500.milliseconds)

        val savedHtml = if (markdownMode) {
            RichTextUtils.markdownToHtml(content.text)
        } else {
            RichTextUtils.toHtml(content.annotatedString)
        }

        onAutoSaveDraft(title, content.text, savedHtml, markdownMode)

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
        delay(1000.milliseconds)

        autoSaveStatus = "Saving…"

        onAutoSave(title, content.text, savedHtml) {
            lastSavedTitle = title
            lastSavedContent = content.text
            lastSavedHtml = savedHtml
            autoSaveStatus = "Saved"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit Note",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = autoSaveStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { applyStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
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

            OutlinedButton(
                onClick = { applyStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
                enabled = !content.selection.collapsed
            ) {
                Text(
                    text = "L",
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

            OutlinedButton(onClick = {
                content = NoteEditorUtils.insertBullet(content)
            }) {
                Text("• List")
            }

            OutlinedButton(onClick = {
                content = NoteEditorUtils.insertChecklistItem(content)
            }) {
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
                onClick = { markdownMode = !markdownMode }
            ) {
                Text(if (markdownMode) "Markdown: ON" else "Markdown")
            }

            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") }
            ) {
                Text("Add Images")
            }

            OutlinedButton(
                onClick = { documentPickerLauncher.launch("*/*") }
            ) {
                Text("Attach Files")
            }

        }

        BasicTextField(
            value = content,
            onValueChange = { newValue ->
                content = NoteEditorUtils.handleNumberedListInput(
                    previous = content,
                    current = newValue
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextfield ->
                Box {
                    if (content.text.isEmpty()) {
                        Text(
                            text = "Write your note",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextfield()
                }
            }
        )


        if (existingAttachments.isNotEmpty()) {
            Text(
                text = "Existing Attachments",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                existingAttachments.forEach { attachment ->

                    if (attachment.fileType.startsWith("image/")) {

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

                    } else {

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
                                            return@clickable
                                        }

                                        val fileUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                fileUri,
                                                attachment.fileType
                                            )
                                            addFlags(
                                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        }

                                        if (
                                            intent.resolveActivity(
                                                context.packageManager
                                            ) != null
                                        ) {
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "No app available to open this file",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Unable to open this attachment",
                                            Toast.LENGTH_SHORT
                                        ).show()
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

                            Column {
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
}