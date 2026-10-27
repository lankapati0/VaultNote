package com.lankeshwar.vaultnote.screens

import android.graphics.Outline
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.Utils.RichTextUtils

@Composable
fun CreateNoteScreen(
    onSave: (String, String, String)-> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }

    fun applyStyle(style: SpanStyle){
        val start = minOf(content.selection.start, content.selection.end)
        val end = maxOf(content.selection.start, content.selection.end)

        if (start == end) return

        val styledText = buildAnnotatedString {
            append(content.annotatedString)
            addStyle(style = style,
                start=start, end = end)
        }

        content = content.copy(annotatedString = styledText)
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
            onValueChange = {content = it},
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
            onClick = {onSave(title,content.text, RichTextUtils.toHtml(content.annotatedString))},
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