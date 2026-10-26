package com.lankeshwar.vaultnote.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun NoteCard(title: String, preview: String,date: String,onClick: () -> Unit, onDelete: () -> Unit,
             onTogglePin:() -> Unit,
             onFavoritePin:() -> Unit,isPinned: Boolean,
             isFavorite: Boolean) {

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable{onClick()},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = preview,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = date,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(
                onClick = {
                    onTogglePin()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if(isPinned) "Unpin note" else "Pin note",
                    tint = if (isPinned) {
                        Color(0xFFFFB300)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = onFavoritePin) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Default.Star
                    } else {
                        Icons.Default.StarBorder
                    },
                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                    tint = if (isFavorite){
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = {
                showDeleteDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete note"
                )
            }
        }
    }
    if(showDeleteDialog){
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {Text("Delete note?")},
            text = {Text("Are you sure you want to delete \"$title\"?")},
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ){Text("Delete")}
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) {Text("Cancel") }
            }
        )
    }
}