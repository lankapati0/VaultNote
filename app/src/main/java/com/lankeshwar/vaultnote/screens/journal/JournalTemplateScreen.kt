package com.lankeshwar.vaultnote.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.data.model.JournalTemplate
import com.lankeshwar.vaultnote.data.model.JournalTemplates

@Composable
fun JournalTemplateScreen(
    onTemplateSelected:(JournalTemplate)-> Unit,
    onStartBlank:()-> Unit,
    onBack:()-> Unit
){
    Column( modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton (onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Choose Journal Template",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton (
            onClick = onStartBlank,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start with Blank Journal")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn (
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            items(JournalTemplates.all){ template ->
                Card (
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onTemplateSelected(template)
                        }
                ){
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Use Template →",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}