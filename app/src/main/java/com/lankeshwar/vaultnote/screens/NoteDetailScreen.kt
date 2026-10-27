package com.lankeshwar.vaultnote.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.viewmodels.TagViewModel

@Composable
fun NoteDetailScreen(
    noteId:Int,
    title: String,
    content: String,
    formattedContent: String,
    date: String,
    onBack:() -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
){
    val tagViewModel: TagViewModel = hiltViewModel()
    val assignedTags by tagViewModel.getTagsForNote(noteId).collectAsState(initial = emptyList())
    val allTags by tagViewModel.tags.collectAsState()
    var showTagDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Note Details",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Note"
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share note"
                )
            }
        }

        Text(text=title, style = MaterialTheme.typography.headlineMedium)

        Text(text=date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val displayContent = if(formattedContent.isNotBlank()){
            RichTextUtils.fromHtml(formattedContent).annotatedString
        } else {
            RichTextUtils.fromHtml(RichTextUtils.markdownToHtml(content)).annotatedString
        }

        Text(text= displayContent, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ){
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium
            )

            Button(onClick = {showTagDialog = true}) {
                Text("Manage")
            }
        }

        if(assignedTags.isEmpty()){
            Text(text="No tags assigned",
                color= MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            assignedTags.forEach { tag ->
                Text(text = "• ${tag.name}",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        if(showTagDialog){
            AlertDialog(
                onDismissRequest = {
                    showTagDialog = false
                },
                title = { "Assign Tags"},
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if(allTags.isEmpty()){
                            Text("No tags available. Create tags in settings.")
                        }

                        allTags.forEach { tag ->
                            val isAssigned = assignedTags.any {
                                it.id == tag.id
                            }

                            TextButton(
                                onClick = {
                                    if(isAssigned){
                                        tagViewModel.removeTagFromNote(noteId,tag.id)
                                    } else {
                                        tagViewModel.addTagToNote(noteId,tag.id)
                                    }
                                }
                            ) {
                                Text(
                                    text = if(isAssigned){
                                        "✓ ${tag.name} (tap to remove)"
                                    } else {
                                        "+ ${tag.name}"
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showTagDialog = false
                    }) {
                        Text("Done")
                    }
                }
            )
        }
    }
    Spacer(modifier = Modifier.padding(top = 16.dp))
}
