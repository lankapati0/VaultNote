package com.lankeshwar.vaultnote.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.Utils.AudioPlayerUtils
import com.lankeshwar.vaultnote.Utils.PrivateAttachmentEncryptionManager
import com.lankeshwar.vaultnote.Utils.RichTextUtils
import com.lankeshwar.vaultnote.data.storage.AttachmentFileManager
import com.lankeshwar.vaultnote.viewmodels.NoteAttachmentViewModel
import com.lankeshwar.vaultnote.viewmodels.TagViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun NoteDetailScreen(
    noteId:Int,
    title: String,
    content: String,
    formattedContent: String,
    date: String,
    isPrivate:Boolean = false,
    onBack:() -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
){
    val tagViewModel: TagViewModel = hiltViewModel()
    val context = LocalContext.current
    val attachmentFileManager = remember(context){ AttachmentFileManager(context) }
    val attachmentViewModel: NoteAttachmentViewModel = hiltViewModel()
    val assignedTags by tagViewModel.getTagsForNote(noteId).collectAsState(initial = emptyList())
    val allTags by tagViewModel.tags.collectAsState()
    var showTagDialog by remember { mutableStateOf(false) }

    val attachments by attachmentViewModel
        .getAttachmentsForNote(noteId).collectAsState(initial = emptyList())

    val audioPlayer = remember { AudioPlayerUtils() }
    var playingFilePath by remember {
        mutableStateOf<String?>(null)
    }

    var isAudioPlaying by remember {
        mutableStateOf(false)
    }
    var activeDecryptedAudioFile by remember { mutableStateOf<File?>(null) }


    val coroutineScope = rememberCoroutineScope()

    fun stopAudioPlayback() {
        audioPlayer.stop { playing ->
            isAudioPlaying = playing
        }
        activeDecryptedAudioFile?.delete()
        activeDecryptedAudioFile = null
        playingFilePath = null
        isAudioPlaying = false
    }

    fun playAudio(filePath: String) {
        coroutineScope.launch {
            try {
                stopAudioPlayback()
                val playbackFile = withContext(Dispatchers.IO){
                    val originalFile = File(filePath)

                    if(isPrivate && PrivateAttachmentEncryptionManager.isEncrypted(originalFile)){
                        attachmentFileManager.createDecryptedTemporaryFile(filePath)
                    } else {
                        originalFile
                    }
                }

                audioPlayer.play(
                    filePath = playbackFile.absolutePath,
                    onPlayingChanged = {playing ->
                        isAudioPlaying = playing
                        if(audioPlayer.currentFilePath != null){
                            playingFilePath = filePath
                        } else {
                            playingFilePath = null
                            activeDecryptedAudioFile?.delete()
                            activeDecryptedAudioFile = null
                        }
                    },
                    onError = {
                        playingFilePath = null
                        if(audioPlayer.currentFilePath == null){
                            activeDecryptedAudioFile?.delete()
                            activeDecryptedAudioFile = null
                        }
                        Toast.makeText(context,"Unable to play this recording", Toast.LENGTH_SHORT).show()
                    }
                )

                activeDecryptedAudioFile = playbackFile.takeIf { it.absolutePath != filePath }
                playingFilePath = filePath
            }catch (e: Exception){
                activeDecryptedAudioFile?.delete()
                activeDecryptedAudioFile = null
                playingFilePath = null
                isAudioPlaying = false

                Toast.makeText(
                    context,
                    "Unable to open this recording",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val latestDecryptedAudioFile by rememberUpdatedState(activeDecryptedAudioFile)

    DisposableEffect (audioPlayer) {
        onDispose {
            audioPlayer.release()
            latestDecryptedAudioFile?.delete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

        if(attachments.isNotEmpty()){
            Text(text="Attachments", style = MaterialTheme.typography.titleMedium)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items=attachments.filter {
                    it.fileType.startsWith("image/")
                }, key = {it.id}){ attachment ->
                    val imageFile by produceState<File?>(
                        initialValue = null,
                        key1 = attachment.filePath,
                        key2 = isPrivate
                    ) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                if (isPrivate &&
                                    PrivateAttachmentEncryptionManager.isEncrypted(
                                        File(attachment.filePath)
                                    )
                                ) {
                                    attachmentFileManager.createDecryptedTemporaryFile(
                                        attachment.filePath
                                    )
                                } else {
                                    File(attachment.filePath)
                                }
                            }.onFailure {error ->
                                Log.e(
                                    "PrivateImage","Failed to load image: ${attachment.filePath}",error)
                            }
                                .getOrNull()
                        }
                    }

                    val bitmap = remember(imageFile) {
                        imageFile?.let { file ->
                            Log.d(
                                "PrivateImage",
                                "File exists=${file.exists()}, size=${file.length()}, path=${file.absolutePath}"
                            )
                            runCatching {
                                BitmapFactory.decodeFile(file.absolutePath)
                            }.onFailure { error ->
                                Log.e("PrivateImage", "Bitmap decoding failed", error)

                            }.getOrNull().also { result ->
                                Log.d(
                                    "PrivateImage",
                                    "Bitmap decoded successfully=${result != null}"
                                )
                            }
                        }
                    }
                    if(bitmap!= null){
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = attachment.fileName,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "Unable to display ${attachment.fileName}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    DisposableEffect(imageFile) {
                        onDispose {
                            if (isPrivate && imageFile!= null && imageFile?.absolutePath != attachment.filePath) {
                                imageFile?.delete()
                            }
                        }
                    }
                }
            }

            attachments.filter { !it.fileType.startsWith("image/") }
                .forEach { attachment ->
                    if(attachment.fileType.startsWith("audio/")){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Audio attachment"
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = attachment.fileName)

                                Text(
                                    text = if (
                                        playingFilePath == attachment.filePath &&
                                        isAudioPlaying
                                    ) {
                                        "Playing…"
                                    } else {
                                        "Audio recording"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (playingFilePath == attachment.filePath) {
                                        if (isAudioPlaying) {
                                            audioPlayer.pause { playing ->
                                                isAudioPlaying = playing
                                            }
                                        } else {
                                            audioPlayer.resume { playing ->
                                                isAudioPlaying = playing
                                            }
                                        }
                                    } else {
                                        playAudio(attachment.filePath)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (
                                        playingFilePath == attachment.filePath &&
                                        isAudioPlaying
                                    ) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = if (
                                        playingFilePath == attachment.filePath &&
                                        isAudioPlaying
                                    ) {
                                        "Pause playback"
                                    } else {
                                        "Play recording"
                                    }
                                )
                            }

                            IconButton(
                                onClick = { stopAudioPlayback() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop playback"
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable{

                                    coroutineScope.launch {
                                        try {
                                            val originalFile = File(attachment.filePath)

                                            if (!originalFile.exists()) {
                                                Toast.makeText(
                                                    context,
                                                    "File not found",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }

                                            val fileToOpen = withContext(Dispatchers.IO) {
                                                if (
                                                    isPrivate &&
                                                    PrivateAttachmentEncryptionManager.isEncrypted(originalFile)
                                                ) {
                                                    attachmentFileManager.createDecryptedTemporaryFile(
                                                        attachment.filePath
                                                    )
                                                } else {
                                                    originalFile
                                                }
                                            }

                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                fileToOpen
                                            )

                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(fileUri, attachment.fileType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }

                                            context.startActivity(
                                                Intent.createChooser(intent, "Open attachment with")
                                            )

                                        } catch (e: android.content.ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "No app available to open this file",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Unable to open this attachment",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ){
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "File Attachment"
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachment.fileName,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = attachment.fileType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                }


        }
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
