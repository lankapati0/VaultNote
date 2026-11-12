package com.lankeshwar.vaultnote.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.viewmodels.NotesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.viewmodels.NoteFilterOption
import com.lankeshwar.vaultnote.viewmodels.NoteSortOption
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.setValue

@Composable
fun NotesScreen(
    onAddNote: () -> Unit,
    onEditNote: (Note) -> Unit,
    notesViewModel: NotesViewModel = viewModel()
) {
    val searchQuery = notesViewModel.searchQuery
    val notes by notesViewModel.notesFlow.collectAsStateWithLifecycle()
    val selectedSort by notesViewModel.selectedSortOption.collectAsStateWithLifecycle()
    val selectedFilter by notesViewModel.selectedFilterOption.collectAsStateWithLifecycle()
    val filteredNotes = notes
    var sortMenuExpanded by remember { mutableStateOf(false) }

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
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = notesViewModel.welcomeMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    notesViewModel.updateSearchQuery(query)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search notes") },
                singleLine = true
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Sorting dropdown
                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true }
                    ) {
                        Text(
                            text = when (selectedSort) {
                                NoteSortOption.NEWEST_FIRST -> "Newest first"
                                NoteSortOption.OLDEST_FIRST -> "Oldest first"
                                NoteSortOption.TITLE_A_TO_Z -> "Title A-Z"
                                NoteSortOption.TITLE_Z_TO_A -> "Title Z-A"
                                NoteSortOption.PINNED_FIRST -> "Pinned first"
                                NoteSortOption.FAVORITES_FIRST -> "Favorites first"
                            }
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Choose sorting"
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        NoteSortOption.entries.forEach { option ->

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            when (option) {
                                                NoteSortOption.NEWEST_FIRST -> "Newest first"
                                                NoteSortOption.OLDEST_FIRST -> "Oldest first"
                                                NoteSortOption.TITLE_A_TO_Z -> "Title A-Z"
                                                NoteSortOption.TITLE_Z_TO_A -> "Title Z-A"
                                                NoteSortOption.PINNED_FIRST -> "Pinned first"
                                                NoteSortOption.FAVORITES_FIRST -> "Favorites first"
                                            }
                                        )

                                        if (selectedSort == option) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected"
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    notesViewModel.updateSortOption(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Filter selector - outside the sorting dropdown
                SingleChoiceSegmentedButtonRow {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        NoteFilterOption.entries.forEach { option ->
                            FilterChip(
                                selected = selectedFilter == option,
                                onClick = {
                                    notesViewModel.updateFilterOption(option)
                                },
                                label = {
                                    Text(
                                        when (option) {
                                            NoteFilterOption.ALL -> "All"
                                            NoteFilterOption.PINNED -> "Pinned"
                                            NoteFilterOption.FAVORITES -> "Favorites"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes.size) { index ->
                        val note = filteredNotes[index]
                        NoteCard(
                            noteId = note.id,
                            title = note.title,
                            preview = note.preview,
                            formattedContent = note.formattedContent,
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