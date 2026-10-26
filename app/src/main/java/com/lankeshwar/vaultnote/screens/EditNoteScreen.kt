package com.lankeshwar.vaultnote.screens

import android.widget.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp


@Composable
fun EditNoteScreen(
    initialTitle: String,
    initialContent: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember(initialTitle) {
        mutableStateOf(initialTitle)
    }

    var content by remember(initialContent) {
        mutableStateOf(initialContent)
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

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Note title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Write your note") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            minLines = 5
        )

        Button(
            onClick = { onSave(title, content) },
            enabled = title.isNotBlank() || content.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Update Note")
        }

        OutlinedButton (
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
