package com.lankeshwar.vaultnote.screens

import android.widget.Button
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.Utils.RichTextUtils


@Composable
fun EditNoteScreen(
    initialTitle: String,
    initialContent: String,
    initialFormattedContent: String,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }

    var content by remember(initialFormattedContent,initialContent) {
        mutableStateOf(
            if(initialFormattedContent.isNotBlank()){
                RichTextUtils.fromHtml(initialFormattedContent)
            } else {
                TextFieldValue(text = initialContent)
            }
        )
    }

    fun applyStyle(style: SpanStyle){
        val start = minOf(content.selection.start, content.selection.end)
        val end = maxOf(content.selection.start, content.selection.end)

        if (start == end) return

        var finalStyle = style

        if(style.textDecoration != null){
            val decorations = mutableListOf<TextDecoration>()

            content.annotatedString.spanStyles
                .filter { it.start < end && it.end > start }
                .forEach { span ->
                    span.item.textDecoration?.let { decoration ->
                        if(decoration.contains(TextDecoration.Underline)){
                            decorations.add(TextDecoration.Underline)
                        }

                        if(decoration.contains(TextDecoration.LineThrough)){
                            decorations.add(TextDecoration.LineThrough)
                        }
                    }
                }

            val newDecoration = style.textDecoration

            if (newDecoration != null) {
                if (newDecoration.contains(TextDecoration.Underline)) {
                    decorations.add(TextDecoration.Underline)
                }

                if (newDecoration.contains(TextDecoration.LineThrough)) {
                    decorations.add(TextDecoration.LineThrough)
                }
            }
            finalStyle = style.copy(
                textDecoration = TextDecoration.combine(
                    decorations.distinct()
                )
            )
        }
        val builder = AnnotatedString.Builder(content.annotatedString)

        builder.addStyle(
            style = finalStyle,
            start = start,
            end = end
        )
/*
        val styledText = buildAnnotatedString {
            append(content.annotatedString)
            addStyle(style = style,
                start=start, end = end)
        }
*/

        content = TextFieldValue(annotatedString = builder.toAnnotatedString(),
        selection = TextRange(start,end)
        )
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }

        BasicTextField(
            value = content,
            onValueChange = {newValue ->
                content = newValue.copy(
                    annotatedString = buildAnnotatedString {
                        append(newValue.text)

                        content.annotatedString.spanStyles.forEach { span ->
                            val start = span.start.coerceIn(0,newValue.text.length)
                            val end = span.end.coerceIn(start,newValue.text.length)

                            if(start<end){
                                addStyle(
                                    style = span.item,
                                    start = start,
                                    end = end
                                )
                            }
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)
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
            onClick = {onSave(title,content.text, RichTextUtils.toHtml(content.annotatedString))},
            enabled = title.isNotBlank() || content.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Update Note")
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }

        }
    }