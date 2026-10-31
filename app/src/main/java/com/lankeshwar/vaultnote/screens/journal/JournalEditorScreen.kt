package com.lankeshwar.vaultnote.screens.journal

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.Utils.JournalPdfUtils
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel

@Composable
fun JournalEditorScreen(date: String,existingEntry: JournalEntity? = null,
                        journalViewModel: JournalViewModel = hiltViewModel()
                        ,onBack:()-> Unit){
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingEntry?.title ?: "") }
    var content by remember { mutableStateOf(existingEntry?.content ?: "") }
    var mood by remember { mutableStateOf(existingEntry?.mood ?: "") }
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

        Text(
            text = "How are you feeling today?",
            style = MaterialTheme.typography.titleSmall
        )
        Row(modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)){

            listOf("😊 Happy",
                "🥰 Loved",
                "😎 Confident",
                "🚀 Productive",
                "🔥 Motivated",
                "😌 Calm",
                "🙏 Grateful",
                "🤩 Excited",
                "😐 Neutral",
                "🤔 Thoughtful",
                "😴 Tired",
                "😟 Worried",
                "😢 Sad",
                "😡 Angry",
                "😰 Stressed",
                "🥱 Bored",
                "💪 Strong",
                "🧘 Peaceful")
                .forEach { option ->
                    FilterChip(
                        selected = mood == option,
                        onClick = {mood=option},
                        label = {Text(option)}
                    )
                }

        }
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
                    journalViewModel.saveEntry(date,title,content,mood,existingEntry)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if(existingEntry != null){
                OutlinedButton(
                    onClick = {

                        val pdfBytes = JournalPdfUtils.createJournalPdf(
                            date=date,mood=mood, title=title,content = content
                        )
                        if(pdfBytes!=null){
                            val savedUri = JournalPdfUtils.saveJournalPdf(
                                context = context,
                                date = date,
                                pdfBytes = pdfBytes
                            )

                            if(savedUri != null){
                                Toast.makeText(
                                    context,
                                    "PDF saved in Documents/VaultNote",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Unable to save PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Unable to generate PDF",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(("Save PDF"))}
            }

            OutlinedButton(
                onClick = {
                    val pdfBytes = JournalPdfUtils.createJournalPdf(
                        date = date,
                        title = title,
                        mood = mood,
                        content = content
                    )

                    if (pdfBytes != null) {
                        JournalPdfUtils.sharePdfBytes(context, pdfBytes)
                    } else {
                        Toast.makeText(
                            context,
                            "Unable to generate PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {Text("Share PDF") }
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