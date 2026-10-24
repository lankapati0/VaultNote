package com.lankeshwar.vaultnote.screens

import android.graphics.Outline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

@Composable
fun CreateNoteScreen(
    onSave: (String, String)-> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

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
        OutlinedTextField(
            value = content,
            onValueChange = {content = it},
            label = {Text("Write your note")},
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Button(
            onClick = {onSave(title,content)},
            enabled = title.isNotBlank() || content.isNotBlank(),
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