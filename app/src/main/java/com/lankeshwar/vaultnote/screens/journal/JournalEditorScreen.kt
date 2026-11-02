package com.lankeshwar.vaultnote.screens.journal

import android.annotation.SuppressLint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lankeshwar.vaultnote.Utils.JournalPdfUtils
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.data.model.JournalPhotoEntity
import com.lankeshwar.vaultnote.data.model.JournalTemplate
import com.lankeshwar.vaultnote.data.storage.JournalPhotoFileManager
import com.lankeshwar.vaultnote.viewmodels.JournalPhotoViewModel
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("RememberReturnType")
@Composable
fun JournalEditorScreen(date: String, existingEntry: JournalEntity? = null,
                        templateToInsert: JournalTemplate? = null,
                        journalViewModel: JournalViewModel = hiltViewModel(),
                        journalPhotoViewModel: JournalPhotoViewModel = hiltViewModel(),
                        onBack:()-> Unit,
                        onChooseTemplate:()-> Unit,
                        onTemplateConsumed:()-> Unit){
    val context = LocalContext.current
    var title by remember(date,existingEntry?.id) { mutableStateOf(existingEntry?.title ?: "") }
    var content by remember(date,existingEntry?.id) { mutableStateOf(existingEntry?.content ?: "") }
    var mood by remember { mutableStateOf(existingEntry?.mood ?: "") }
    var showDeleteConfirmation by remember {mutableStateOf(false)}
    val photoFileManager = remember { JournalPhotoFileManager(context) }
    val coroutineScope  = rememberCoroutineScope()

    var selectedPhotoUris by remember(date,existingEntry?.id) {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        selectedPhotoUris = (selectedPhotoUris + uris).distinct()
    }

    val savedPhotosFlow = remember(existingEntry?.id){existingEntry?.let {
        journalPhotoViewModel.getPhotosByJournalId(it.id)
        }
    }

    val savedPhotos by (savedPhotosFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    LaunchedEffect(templateToInsert) {
        if (templateToInsert != null){
            val propmpts = templateToInsert.prompts
            content = when {
                content.isBlank() -> propmpts
                content.contains(propmpts) -> content
                else -> "$content\n\n$propmpts"
            }
        }
        onTemplateConsumed()
    }


    Column( modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
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

        OutlinedButton(onClick = onChooseTemplate,
            modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Choose Template")
        }

        OutlinedButton(onClick = {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
            modifier = Modifier.fillMaxWidth()
        ) {Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null
        )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Photos")
        }

        if(selectedPhotoUris.isNotEmpty() || savedPhotos.isNotEmpty()){
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedPhotoUris) { uri ->

                    Box(
                        modifier = Modifier.size(100.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Journal photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = {
                                selectedPhotoUris = selectedPhotoUris - uri
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White
                            )
                        }
                    }
                }
                items(savedPhotos) { photo ->

                    Box(
                        modifier = Modifier.size(100.dp)
                    ){
                        AsyncImage(
                            model = File(photo.filePath),
                            contentDescription = "Saved journal photo",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    photoFileManager.deletePhoto(photo.filePath)
                                    journalPhotoViewModel.deletePhotoAndWait(photo)
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove saved photo",
                                tint = Color.White
                            )
                        }

                    }
                }

            }
            Spacer(modifier = Modifier.height(12.dp))
        }

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
                    coroutineScope.launch {
                        try{
                            val journalId = journalViewModel.saveEntryAndGetId(
                                date = date,
                                title = title,content=content,mood=mood,
                                existingEntry = existingEntry
                            )

                            selectedPhotoUris.forEach { uri ->
                                val photoFile = photoFileManager.copyPhoto(uri)

                                journalPhotoViewModel.insertPhotoAndWait(
                                    JournalPhotoEntity(
                                        journalId = journalId.toInt(),
                                        filePath = photoFile.absolutePath
                                    )
                                )
                            }
                            onBack()
                        }catch (e: Exception){
                            Toast.makeText(
                                context,
                                "Unable to save journal photos",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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
                            date=date,mood=mood, title=title,content = content,
                            photoPaths = savedPhotos.map { it.filePath }
                        )
                        if(pdfBytes!=null){
                            val savedUri = JournalPdfUtils.saveJournalPdf(
                                context = context,
                                date = date,
                                pdfBytes = pdfBytes,
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
                        content = content,
                        photoPaths = savedPhotos.map { it.filePath }
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
                            coroutineScope.let {
                                savedPhotos.forEach { photo ->
                                    photoFileManager.deletePhoto(photo.filePath)
                                }
                                existingEntry?.let {
                                    journalViewModel.deleteEntry(it)
                                }
                                showDeleteConfirmation = false
                                onBack()

                            }
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