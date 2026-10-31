package com.lankeshwar.vaultnote.screens.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel

@Composable
fun JournalEditorScreen(date: String,existingEntry: JournalEntity? = null,
                        journalViewModel: JournalViewModel = hiltViewModel()
                        ,onBack:()-> Unit){

    var title by remember { mutableStateOf(existingEntry?.title ?: "") }
    var content by remember { mutableStateOf(existingEntry?.content ?: "") }
    var showDeleteConfirmation by remember {mutableStateOf(false)}

    Column( modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = {title = it},
            modifier = Modifier.fillMaxWidth(),
            label = {Text("Journal Title")}
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = content,
            onValueChange = {content = it},
            modifier = Modifier.fillMaxWidth().weight(1f),
            minLines = 8,
            label = {Text("Write about your day...")}
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ){
            Button(
                onClick = {
                    journalViewModel.saveEntry(date,title,content,existingEntry)
                    onBack()
                },
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank() || content.isNotBlank()
            ) {
                Text("Save Journal")
            }

            if(existingEntry != null){
                OutlinedButton({showDeleteConfirmation = true}, modifier = Modifier.weight(1f)) {
                    Text("Delete")
                }
            }
        }

        if(showDeleteConfirmation){
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                },
                title = {
                    Text("Delete Journal Entry?")
                },
                text = {
                    Text("This journal entry will be permanently deleted.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            existingEntry?.let {
                                journalViewModel.deleteEntry(it)
                            }
                            showDeleteConfirmation = false
                            onBack()
                        }
                    ) { Text("Delete")}
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                        }
                    ) { Text("Cancel")}
                }
            )
        }
    }
}