package com.lankeshwar.vaultnote.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lankeshwar.vaultnote.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.Utils.ReminderScheduler
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.data.repository.NoteAttachmentRepository
import com.lankeshwar.vaultnote.data.repository.NoteDraftRepository
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.data.storage.AttachmentFileManager
import com.lankeshwar.vaultnote.data.storage.VoiceRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.emptyList

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val draftRepository: NoteDraftRepository,
    private val attachmentRepository: NoteAttachmentRepository,
    @ApplicationContext private val context: Context
): ViewModel() {
    val welcomeMessage:String
        get() = repository.getWelcomeMessage()
    private val searchQueryFlow = MutableStateFlow("")

    private val attachmentFileManager = AttachmentFileManager(context)
    private val voiceRecorder = VoiceRecorder(context)
    var searchQuery by mutableStateOf("")
    private set
    private val notes = repository.getRegularNotes()
        .combine(searchQueryFlow){entities, query ->
            entities.map { entity ->
                Note(
                    id = entity.id,
                    title = entity.title,
                    preview = entity.preview,
                    formattedContent = entity.formattedContent,
                    date = entity.date,
                    isPrivate = entity.isPrivate,
                    isPinned = entity.isPinned,
                    isFavorite = entity.isFavorite,
                    reminderTime =  entity.reminderTime,
                    reminderRepeat = entity.reminderRepeat,
                    isReminderEnabled = entity.isReminderEnabled
                )
            }.filter { note ->
                note.title.contains(query, ignoreCase = true) || note.preview.contains(query, ignoreCase = true)
            }.sortedWith (
                compareByDescending<Note> {it.isPinned  }
                    .thenByDescending { it.id }
            )

        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentlyDeletedFlow: StateFlow<List<Note>> =
        repository.getRecentlyDeletedNotes()
            .map { entities ->
                entities.map { entity ->
                    Note(
                        id = entity.id,
                        title = entity.title,
                        preview = entity.preview,
                        formattedContent = entity.formattedContent,
                        date = entity.date,
                        isPrivate = entity.isPrivate,
                        isPinned = entity.isPinned,
                        isFavorite = entity.isFavorite,
                        reminderTime =  entity.reminderTime,
                        reminderRepeat = entity.reminderRepeat,
                        isReminderEnabled = entity.isReminderEnabled
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val privateNotesFlow: StateFlow<List<Note>> =
        repository.getPrivateNotes()
            .map { entities ->
                entities.map { entity ->
                    Note(
                        id = entity.id,
                        title = entity.title,
                        preview = entity.preview,
                        formattedContent = entity.formattedContent,
                        date = entity.date,
                        isPrivate = entity.isPrivate,
                        isFavorite = entity.isFavorite,
                        isPinned = entity.isPinned,
                        reminderTime =  entity.reminderTime,
                        reminderRepeat = entity.reminderRepeat,
                        isReminderEnabled = entity.isReminderEnabled
                    )
                }
            }
            .combine(searchQueryFlow) { notes, query ->
                notes.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                            note.preview.contains(query, ignoreCase = true)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )



    val notesFlow = notes
    fun updateSearchQuery(query: String){
        searchQuery = query
        searchQueryFlow.value = query
    }

    fun addNote(title: String, content: String, isPrivate: Boolean = false, formattedContent: String = content,
                onNoteSaved: ((Long)-> Unit)? = null, onComplete: (() -> Unit)? = null){
        if(title.isBlank() && content.isBlank()) return

        viewModelScope.launch {

            val noteId = repository.insertNote(
                NoteEntity(
                    id= 0,
                    title = title.ifBlank { "Untitled Note" },
                    preview = content,
                    formattedContent = formattedContent,
                    date = "Today",
                    isPrivate = isPrivate
                )
            )
            onNoteSaved?.invoke(noteId)
            onComplete?.invoke()
        }
    }

    fun deleteNotes(note:Note){
        viewModelScope.launch {
            repository.moveToRecentlyDeleted(note.id)
        }
    }

    fun updateNote(note:Note,
                   title: String,
                   content: String,
                   formattedContent:String = content,
                   onComplete:(()-> Unit)? = null){
        if(title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            repository.updateNote(
                NoteEntity(
                    id = note.id,
                    title = title.ifBlank { "Untitled Note" },
                    preview = content,
                    formattedContent = formattedContent,
                    date = note.date,
                    isPrivate = note.isPrivate,
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    reminderTime =  note.reminderTime,
                    reminderRepeat = note.reminderRepeat,
                    isReminderEnabled = note.isReminderEnabled
                )
            )
            onComplete?.invoke()
        }
    }

    fun togglePin(note: Note){
        viewModelScope.launch {
            val newPinnedState = !note.isPinned

            repository.updatePinnedStatus(
                noteId = note.id,
                isPinned = newPinnedState
            )
        }
    }

    fun toggleFavorite(note:Note){
        viewModelScope.launch {
            repository.updateFavoriteStatus(
                noteId = note.id,
                isFavorite = !note.isFavorite
            )
        }
    }

    fun restoreNote(note: Note){
        viewModelScope.launch {
            repository.restoreNote(note.id)
        }
    }

    fun permanentlyDeleteNote(note: Note){
        viewModelScope.launch {
            repository.permanentlyDeleteNote(note.id)
        }
    }

    fun saveDraft(draftId: String,
                  title: String,
                  content: String,
                  formattedContent: String,
                  isMarkdown: Boolean){
        viewModelScope.launch {
            draftRepository.saveDraft(
                NoteDraftEntity(
                    draftId = draftId,
                    title = title,
                    content = content,
                    formattedContent = formattedContent,
                    isMarkdown = isMarkdown,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getDraft(draftId: String): NoteDraftEntity? {
        return draftRepository.getDraft(draftId)
    }

    fun deleteDraft(draftId: String){
        viewModelScope.launch {
            draftRepository.deleteDraft(draftId)
        }
    }

    fun saveImageAttachments(noteId: Long, imageUris: List<Uri>){
        viewModelScope.launch {
            imageUris.forEach { uri ->
                try {
                    val fileName = getAttachmentFileManager(uri)
                    val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                    val savedFile = attachmentFileManager.copyAttachment(uri= uri, originalFileName = fileName)

                    attachmentRepository.addAttachment(
                        NoteAttachmentEntity(
                            noteId = noteId.toInt(),
                            fileName = fileName,
                            fileType = mimeType,
                            filePath = savedFile.absolutePath
                        )
                    )

                } catch (e: Exception){
                    Log.e("NoteAttachments","Failed to save image attachment", e)
                }
            }
        }
    }

    fun saveFileAttachments(noteId:Long, fileUris:List<Uri>){
        viewModelScope.launch {
            fileUris.forEach { uri ->
                try {
                    val fileName = getAttachmentFileManager(uri)
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                    val savedFile = attachmentFileManager.copyAttachment(uri=uri, originalFileName = fileName)

                    attachmentRepository.addAttachment(
                        NoteAttachmentEntity(
                            noteId = noteId.toInt(),
                            fileName = fileName,
                            fileType = mimeType,
                            filePath = savedFile.absolutePath
                        )
                    )
                }catch (e: Exception){
                    Log.e("NoteAttachments",
                        "Failed to save document attachment",
                        e)
                }
            }
        }
    }

    private fun getAttachmentFileManager(uri: Uri): String {
        var fileName:String? = null

        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null,null,null)?.use { cursor ->
            if(cursor.moveToFirst()){
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if(columnIndex >= 0){
                    fileName = cursor.getString(columnIndex)
                }
            }
        }
        return fileName ?: uri.lastPathSegment ?: "image"
    }

    fun startVoiceRecording():String {
        return voiceRecorder.startRecording().absolutePath
    }

    fun stopVoiceRecording(noteId: Long){
        viewModelScope.launch {
            val recordedFile = voiceRecorder.stopRecording() ?: return@launch
            val attachment = NoteAttachmentEntity(
                noteId = noteId.toInt(),
                fileName = recordedFile.name,
                fileType = "audio/mp4",
                filePath = recordedFile.absolutePath
            )
            attachmentRepository.addAttachment(attachment)
        }
    }

    fun stopVoiceRecordingForNewNote(): File? {
        return voiceRecorder.stopRecording()
    }

    fun cancelVoiceRecording(){
        voiceRecorder.cancelRecording()
    }

    fun saveVoiceRecording(noteId:Long, recordedFile:File){
        viewModelScope.launch {
            val attachment = NoteAttachmentEntity(
                noteId = noteId.toInt(),
                fileName = recordedFile.name,
                fileType = "audio/mp4",
                filePath = recordedFile.absolutePath
            )
            attachmentRepository.addAttachment(attachment)
        }
    }

    fun saveNoteReminder(noteId:Long, title:String, reminderTime: Long?,reminderRepeat: String,isEnabled: Boolean){
        val id = noteId.toInt()
        viewModelScope.launch {
            repository.updateReminder(
                noteId = id,
                reminderTime = reminderTime,
                reminderRepeat = reminderRepeat,
                isEnabled = isEnabled
            )
            val scheduler = ReminderScheduler(context)

            if (isEnabled && reminderTime != null && reminderTime > System.currentTimeMillis()) {
                scheduler.scheduleReminder(
                    noteId = id,
                    title = title,
                    reminderTime = reminderTime,
                    reminderRepeat = reminderRepeat
                )
            } else {
                scheduler.cancelReminder(id)
            }
        }
    }

    fun updateReminder(noteId:Int,title: String, reminderTime:Long?, reminderRepeat:String, isEnabled: Boolean, onComplete:()-> Unit={}) {
        val id = noteId.toInt()
        viewModelScope.launch {
            repository.updateReminder(
                noteId = noteId,
                reminderTime = reminderTime,
                reminderRepeat = reminderRepeat,
                isEnabled = isEnabled
            )
            onComplete()
        }
    }

    fun disableOneTimeReminder(noteId:Int){
        viewModelScope.launch {
            repository.disableOneTimeReminder(noteId)
        }
    }




}