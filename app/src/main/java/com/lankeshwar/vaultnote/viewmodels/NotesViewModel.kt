package com.lankeshwar.vaultnote.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.lankeshwar.vaultnote.data.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.Note
import com.lankeshwar.vaultnote.data.NoteDraftRepository
import com.lankeshwar.vaultnote.data.db.NoteDraftEntity
import com.lankeshwar.vaultnote.data.db.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val draftRepository: NoteDraftRepository
): ViewModel() {
    val welcomeMessage:String
        get() = repository.getWelcomeMessage()
    private val searchQueryFlow = MutableStateFlow("")

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
                    isFavorite = entity.isFavorite
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
                        isFavorite = entity.isFavorite
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
                        isPinned = entity.isPinned
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

    fun addNote(title: String, content: String, isPrivate: Boolean = false,formattedContent: String = content,onComplete: (() -> Unit)?){
        if(title.isBlank() && content.isBlank()) return

        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    id= 0,
                    title = title.ifBlank { "Untitled Note" },
                    preview = content,
                    formattedContent = formattedContent,
                    date = "Today",
                    isPrivate = isPrivate
                )
            )
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

}