package com.lankeshwar.vaultnote.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.data.Note

@Composable
fun NotesScreen(
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    notesViewModel: NotesViewModel = viewModel()
) {
    val searchQuery = notesViewModel.searchQuery
    val notes by notesViewModel.notesFlow.collectAsStateWithLifecycle()
    val filteredNotes = notes

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAddNote()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add note"
                )
            }
        }
    ) {innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
            , verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "My Notes",
                style = MaterialTheme.typography.headlineLarge

            )
            Text(
                text = notesViewModel.welcomeMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {query ->
                    notesViewModel.updateSearchQuery(query)
                },
                modifier = Modifier.fillMaxWidth(),
                label = {Text("Search notes")},
                singleLine = true
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes.size){ index ->
                    val note = filteredNotes[index]
                    NoteCard(
                        noteId = note.id,
                        title = note.title,
                        preview = note.preview,
                        date = note.date,
                        onDelete = {
                            notesViewModel.deleteNotes(note)
                        },
                        onClick = {
                            onEditNote(note)
                        },
                        onTogglePin = {
                            notesViewModel.togglePin(note)
                        },
                        onFavoritePin = {
                            notesViewModel.toggleFavorite(note)
                        },
                        isPinned = note.isPinned,
                        isFavorite = note.isFavorite
                    )
                }
            }
        }
    }
}