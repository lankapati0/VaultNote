package com.lankeshwar.vaultnote.screens.SettingSecurity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.viewmodels.TagViewModel
import androidx.core.graphics.toColorInt
import com.lankeshwar.vaultnote.data.db.entity.TagEntity

@Composable
fun TagManagementScreen(
    tagViewModel: TagViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val tags by tagViewModel.tags.collectAsState()
    val tagMessage by tagViewModel.tagMessage.collectAsState()

    var tagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0A84FF") }
    var editingTag by remember { mutableStateOf<TagEntity?>(null) }

    val tagColors = listOf(
        "#FF453A", // Red
        "#FF9F0A", // Orange
        "#FFD60A", // Yellow
        "#30D158", // Green
        "#0A84FF", // Blue
        "#BF5AF2", // Purple
        "#8E8E93"  // Gray
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Manage Tags",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tag name
        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            label = { Text("Tag Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Color selector for new tags
        Text(
            text = "Choose a color",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tagColors.forEach { hex ->
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { selectedColor = hex },
                    shape = CircleShape,
                    color = Color(
                        hex.toColorInt()
                    ),
                    border = if (selectedColor == hex) {
                        BorderStroke(
                            3.dp,
                            MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        null
                    }
                ) {}
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                tagViewModel.createTag(tagName, selectedColor)
                tagName = ""
            },
            enabled = tagName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Tag")
        }

        // Success/error message
        if (tagMessage != null) {
            Text(
                text = tagMessage!!,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Existing tags
        if (tags.isEmpty()) {
            Text(
                text = "No tags yet. Create your first tag",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = tags,
                    key = { it.id }
                ) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Colored tag label
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(
                                tag.colorHex.toColorInt()
                            )
                        ) {
                            Text(
                                text = tag.name,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit color
                            IconButton(
                                onClick = {
                                    editingTag = tag
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit tag color"
                                )
                            }

                            // Delete tag
                            IconButton(
                                onClick = {
                                    tagViewModel.deleteTag(tag.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete tag"
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    // Dialog is outside LazyColumn
    editingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = {
                editingTag = null
            },
            title = {
                Text("Change Tag Color")
            },
            text = {
                Column {
                    Text("Choose a new color for ${tag.name}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tagColors.forEach { hex ->
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        tagViewModel.updateTagColor(
                                            tag.id,
                                            hex
                                        )
                                        editingTag = null
                                    },
                                shape = CircleShape,
                                color = Color(
                                    hex.toColorInt()
                                ),
                                border = if (tag.colorHex == hex) {
                                    BorderStroke(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    null
                                }
                            ) {}
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingTag = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}