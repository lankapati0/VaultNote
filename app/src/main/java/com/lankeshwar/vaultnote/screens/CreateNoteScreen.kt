package com.lankeshwar.vaultnote.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.substring
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import com.lankeshwar.vaultnote.Utils.NoteEditorUtils
import com.lankeshwar.vaultnote.Utils.NoteEditorUtils.insertBullet
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.data.db.NoteDraftEntity
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CreateNoteScreen(
    onSave: (String, String, String)-> Unit,
    onCancel: () -> Unit,
    onAutoSave: (String, String, String, Boolean) -> Unit,
    onLoadDraft: suspend () -> NoteDraftEntity?,
    onClearDraft: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var markdownMode by remember { mutableStateOf(false) }

    var isDraftLoaded by remember { mutableStateOf(false) }

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
            return@LaunchedEffect
        }
        delay(1000.milliseconds)
        val savedHtml = if(markdownMode){
            RichTextUtils.markdownToHtml(content.text)
        } else {
            RichTextUtils.toHtml(content.annotatedString)
        }

        onAutoSave(title,content.text,savedHtml, markdownMode)
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Note")

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
                onClick = {applyHeading(28)},
                enabled = !content.selection.collapsed
            ) {
                Text("H1")
            }

            OutlinedButton(
                onClick = {applyHeading(24)},
                enabled = !content.selection.collapsed
            ) {
                Text("H2")
            }

            OutlinedButton(
                onClick = {applyHeading(20)},
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


                onSave(title,content.text, savedHtml)},
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