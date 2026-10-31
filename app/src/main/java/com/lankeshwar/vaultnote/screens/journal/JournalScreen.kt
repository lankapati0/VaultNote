package com.lankeshwar.vaultnote.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel

@Composable
fun JournalScreen(
    journalViewModel: JournalViewModel = hiltViewModel(),
    onAddEntry:() -> Unit,
    onEditEntry:(JournalEntity)-> Unit
) {
    val entries by journalViewModel.journalEntries.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Your Daily Journal",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onAddEntry,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Write Today's Journal")}

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Text("No journal entries yet. Start writing today!")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = entries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEditEntry(entry)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = entry.date,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = entry.title.ifBlank { "Untitled Entry" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = entry.content,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}