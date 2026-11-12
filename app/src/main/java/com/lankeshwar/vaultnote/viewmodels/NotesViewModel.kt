package com.lankeshwar.vaultnote.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import com.lankeshwar.vaultnote.data.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.Utils.PrivateAttachmentEncryptionManager
import com.lankeshwar.vaultnote.Utils.ReminderScheduler
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.data.repository.NoteAttachmentRepository
import com.lankeshwar.vaultnote.data.repository.NoteDraftRepository
import com.lankeshwar.vaultnote.data.db.entity.NoteAttachmentEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity
import com.lankeshwar.vaultnote.data.db.entity.NoteEntity
import com.lankeshwar.vaultnote.data.storage.AttachmentFileManager
import com.lankeshwar.vaultnote.data.storage.VoiceRecorder
import com.lankeshwar.vaultnote.screens.private_note_screen.PrivateNoteSession
import com.lankeshwar.vaultnote.widget.VaultNoteWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.emptyList

enum class NoteSortOption {
    NEWEST_FIRST,
    OLDEST_FIRST,
    TITLE_A_TO_Z,
    TITLE_Z_TO_A,
    PINNED_FIRST,
    FAVORITES_FIRST
}
enum class NoteFilterOption {
    ALL, PINNED, FAVORITES
}
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val draftRepository: NoteDraftRepository,
    private val attachmentRepository: NoteAttachmentRepository,
    private val privateNoteSession: PrivateNoteSession,
    @ApplicationContext private val context: Context
): ViewModel() {

    val welcomeMessage:String
        get() = repository.getWelcomeMessage()
    private val searchQueryFlow = MutableStateFlow("")
    private val sortOptionFlow = MutableStateFlow(NoteSortOption.PINNED_FIRST)
    private val filterOptionFlow = MutableStateFlow(NoteFilterOption.ALL)
    val selectedSortOption: StateFlow<NoteSortOption> = sortOptionFlow
    val selectedFilterOption: StateFlow<NoteFilterOption> = filterOptionFlow

    fun updateSortOption(option: NoteSortOption){
        sortOptionFlow.value = option
    }

    fun updateFilterOption(option: NoteFilterOption){
        filterOptionFlow.value = option
    }

    private val attachmentFileManager = AttachmentFileManager(context)
    private val voiceRecorder = VoiceRecorder(context)
    var searchQuery by mutableStateOf("")
    private set

    val isPrivateNoteUnlockd = privateNoteSession.isUnlocked

    fun unlockPrivateNotes(){
        privateNoteSession.unlock()
        viewModelScope.launch {
            repository.migrateExistingPrivateNote()
            migrateExistingPrivateAttachments()
        }
        android.util.Log.d(
            "VaultNoteBackup",
            "NotesViewModel unlocked: ${privateNoteSession.isUnlocked.value}"
        )
    }

    fun lockPrivateNotes(){
        android.util.Log.d(
            "VaultNoteBackup",
            "Private notes LOCK requested"
        )
        privateNoteSession.lock()
    }

    private val notes = combine(
        repository.getRegularNotes(),
        searchQueryFlow,
        sortOptionFlow,
        filterOptionFlow
    ) { entities, query, sortOption, filterOption ->

        filterAndSortNotes(
            entities = entities,
            query = query,
            sortOption = sortOption,
            filterOption = filterOption
        )

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val recentlyDeletedFlow: StateFlow<List<Note>> =
        repository.getRecentlyDeletedNotes()
            .combine(privateNoteSession.isUnlocked) { entities, isUnlocked ->

                entities
                    .filter { entity ->
                        !entity.isPrivate || isUnlocked
                    }
                    .map { entity ->
                        Note(
                            id = entity.id,
                            title = entity.title,
                            preview = entity.preview,
                            formattedContent = entity.formattedContent,
                            date = entity.date,
                            isPrivate = entity.isPrivate,
                            isPinned = entity.isPinned,
                            isFavorite = entity.isFavorite,
                            reminderTime = entity.reminderTime,
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
            .combine(searchQueryFlow) { entities, query ->
                entities to query
            }
            .combine(sortOptionFlow) { (entities, query), sortOption ->
                Triple(entities, query, sortOption)
            }
            .combine(filterOptionFlow) { (entities, query, sortOption), filterOption ->

                filterAndSortNotes(
                    entities = entities,
                    query = query,
                    sortOption = sortOption,
                    filterOption = filterOption
                )
            }
            .combine(privateNoteSession.isUnlocked) { filteredNotes, isUnlocked ->
                if (isUnlocked) filteredNotes else emptyList()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val notesFlow = notes

    fun updateSearchQuery(query: String) {
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
            VaultNoteWidget().updateAll(context)
            onNoteSaved?.invoke(noteId)
            onComplete?.invoke()
        }
    }

    fun deleteNotes(note:Note){
        viewModelScope.launch {
            repository.moveToRecentlyDeleted(note.id)
            VaultNoteWidget().updateAll(context)
        }
    }

    fun updateNote(note:Note,
                   title: String,
                   content: String,
                   formattedContent:String = content,
                   onComplete:(()-> Unit)? = null){
        if(title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            val existingNote = repository.getNoteById(note.id) ?: return@launch
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
                    isReminderEnabled = note.isReminderEnabled,
                    updatedAt = System.currentTimeMillis(),
                    cloudId = existingNote.cloudId,
                    cloudOwnerId = existingNote.cloudOwnerId,
                    lastSyncedAt = existingNote.lastSyncedAt,
                    isCloudDeleted = existingNote.isCloudDeleted,
                    deletedAt = existingNote.deletedAt
                )
            )
            VaultNoteWidget().updateAll(context)
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
        if(note.isPrivate && !privateNoteSession.isUnlocked.value){
            return
        }

        viewModelScope.launch {
            repository.restoreNote(note.id)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            try {
                val attachments =
                    attachmentRepository.getAttachmentsForDeletion(note.id)

                // Delete physical attachment files first.
                for (attachment in attachments) {
                    val deleted = attachmentFileManager.deleteAttachment(
                        attachment.filePath
                    )

                    if (!deleted) {
                        Log.e(
                            "NoteDeletion",
                            "Failed to delete attachment: ${attachment.filePath}"
                        )
                        return@launch
                    }
                }

                // Delete attachment records from Room.
                attachmentRepository.deleteAttachmentForNote(note.id)

                // Finally, permanently delete the note.
//                repository.permanentlyDeleteNote(note.id)
                repository.markNoteAsCloudDeleted(note.id)
                VaultNoteWidget().updateAll(context)

            } catch (e: Exception) {
                Log.e("NoteDeletion", "Failed to permanently delete note", e)
            }
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
            try {
                val isPrivate = repository.isPrivateNote(noteId.toInt()) ?: throw IllegalStateException("Note not found")

                imageUris.forEach { uri ->
                    try {
                        val fileName = getAttachmentFileManager(uri)
                        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
                        val savedFile = if(isPrivate){
                            attachmentFileManager.copyPrivateAttachment(uri=uri, originalFileName = fileName)
                        } else {
                            attachmentFileManager.copyAttachment(uri=uri, originalFileName = fileName)
                        }
                        attachmentRepository.addAttachment(
                            NoteAttachmentEntity(
                                noteId = noteId.toInt(),
                                fileName = fileName,
                                fileType = mimeType,
                                filePath = savedFile.absolutePath
                            )
                        )
                    }catch (e: Exception){
                        Log.e("NoteAttachments", "Failed to save image attachment", e)
                    }
                }

            }catch (e: Exception){
                Log.e("NoteAttachments", "Unable to check note privacy", e)
            }
        }
    }


    fun saveFileAttachments(noteId: Long, fileUris: List<Uri>) {
        viewModelScope.launch {
            try {
                val isPrivate = repository.isPrivateNote(noteId.toInt())
                    ?: throw IllegalStateException("Note not found.")

                fileUris.forEach { uri ->
                    try {
                        val fileName = getAttachmentFileManager(uri)
                        val mimeType =
                            context.contentResolver.getType(uri)
                                ?: "application/octet-stream"

                        val savedFile = if (isPrivate) {
                            attachmentFileManager.copyPrivateAttachment(
                                uri = uri,
                                originalFileName = fileName
                            )
                        } else {
                            attachmentFileManager.copyAttachment(
                                uri = uri,
                                originalFileName = fileName
                            )
                        }

                        attachmentRepository.addAttachment(
                            NoteAttachmentEntity(
                                noteId = noteId.toInt(),
                                fileName = fileName,
                                fileType = mimeType,
                                filePath = savedFile.absolutePath
                            )
                        )

                    } catch (e: Exception) {
                        Log.e(
                            "NoteAttachments",
                            "Failed to save document attachment",
                            e
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("NoteAttachments", "Unable to check note privacy", e)
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

            var encryptedFile: File? = null
            try {
                val isPrivate = repository.isPrivateNote(noteId.toInt())
                    ?: throw IllegalStateException("Note note found")

                if(isPrivate){
                    encryptedFile = attachmentFileManager.encryptPrivateAttachmentFile(recordedFile)
                }
                val fileToSave = encryptedFile ?: recordedFile

                val attachment = NoteAttachmentEntity(
                    noteId = noteId.toInt(),
                    fileName = recordedFile.name,
                    fileType = "audio/mp4",
                    filePath = fileToSave.absolutePath
                )

                attachmentRepository.addAttachment(attachment)
                if(isPrivate){
                    recordedFile.delete()
                }
            }catch (e: Exception){
                encryptedFile?.delete()
                Log.e(
                    "NoteAttachments",
                    "Failed to save voice recording",
                    e)
            }


        }
    }

    fun stopVoiceRecordingForNewNote(): File? {
        return voiceRecorder.stopRecording()
    }

    fun cancelVoiceRecording(){
        voiceRecorder.cancelRecording()
    }

    fun migrateExistingPrivateAttachments(){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val attachments = attachmentRepository.getAllPrivateNoteAttachments()

                for(attachment in attachments){
                    val originalFile = File(attachment.filePath)
                    if(PrivateAttachmentEncryptionManager.isEncrypted(originalFile)){
                        continue
                    }
                    if(!originalFile.exists()){
                        Log.e("AttachmentMigration", "Attachment file not found: ${attachment.filePath}")
                        continue
                    }
                    var encryptedFile: File? = null
                    try {
                        encryptedFile = attachmentFileManager.encryptPrivateAttachmentFile(originalFile)
                        attachmentRepository.updateAttachmentFilePath(
                            attachmentId = attachment.id,
                            newFilePath = encryptedFile.absoluteFile
                        )

                        if(!attachmentFileManager.deleteAttachment(originalFile.absolutePath)){
                            Log.e("AttachmentMigration", "Encrypted successfully, but could not delete plaintext file: ${originalFile.absolutePath}")
                        }
                    }catch (e: Exception){
                        encryptedFile?.delete()
                        Log.e("AttachmentMigration", "Failed to migrate attachment ${attachment.id}", e)
                    }
                }
            }catch (e: Exception){
                Log.e("AttachmentMigration","Unable to retrieve private attachments",e)
            }
        }
    }


    fun saveVoiceRecording(noteId: Long, recordedFile: File) {
        viewModelScope.launch {
            var encryptedFile: File? = null

            try {
                val isPrivate = repository.isPrivateNote(noteId.toInt())
                    ?: throw IllegalStateException("Note not found.")

                if (isPrivate) {
                    encryptedFile =
                        attachmentFileManager.encryptPrivateAttachmentFile(
                            recordedFile
                        )
                }

                val fileToSave = encryptedFile ?: recordedFile

                attachmentRepository.addAttachment(
                    NoteAttachmentEntity(
                        noteId = noteId.toInt(),
                        fileName = recordedFile.name,
                        fileType = "audio/mp4",
                        filePath = fileToSave.absolutePath
                    )
                )

                if (isPrivate) {
                    recordedFile.delete()
                }

            } catch (e: Exception) {
                encryptedFile?.delete()

                Log.e(
                    "NoteAttachments",
                    "Failed to save voice recording",
                    e
                )
            }
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

    private fun filterAndSortNotes(entities:List<NoteEntity>,
                                   query: String, sortOption: NoteSortOption,
                                    filterOption: NoteFilterOption
                                   ): List<Note>{

        return entities.map{entity ->
            Note(id = entity.id,
                title = entity.title, preview = entity.preview, formattedContent = entity.formattedContent,
                date = entity.date, isPrivate = entity.isPrivate, isPinned = entity.isPinned,
                isFavorite = entity.isFavorite, reminderTime = entity.reminderTime,
                isReminderEnabled = entity.isReminderEnabled,
                reminderRepeat = entity.reminderRepeat
            )
        }
            .filter { note ->
                query.isBlank()|| note.title.contains(query, ignoreCase = true) ||
                        note.preview.contains(query, ignoreCase = true) ||
                        note.formattedContent.contains(query, ignoreCase = true)
            }
            .filter { note ->
                when(filterOption){
                    NoteFilterOption.ALL -> true
                    NoteFilterOption.PINNED -> note.isPinned
                    NoteFilterOption.FAVORITES -> note.isFavorite
                }
            }.let { filterNotes ->
                when(sortOption){
                    NoteSortOption.NEWEST_FIRST -> filterNotes.sortedByDescending { it.id }
                    NoteSortOption.OLDEST_FIRST -> filterNotes.sortedBy { it.id }
                    NoteSortOption.TITLE_A_TO_Z -> filterNotes.sortedBy { it.title.lowercase() }
                    NoteSortOption.TITLE_Z_TO_A -> filterNotes.sortedByDescending { it.title.lowercase() }
                    NoteSortOption.PINNED_FIRST -> filterNotes.sortedWith(compareByDescending<Note> {it.isPinned}
                        .thenByDescending { it.id })

                    NoteSortOption.FAVORITES_FIRST -> filterNotes.sortedWith(compareByDescending<Note> {it.isFavorite}
                        .thenByDescending { it.id })
                }
            }
    }

}