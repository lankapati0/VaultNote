package com.lankeshwar.vaultnote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.TagRepository
import com.lankeshwar.vaultnote.data.db.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repository: TagRepository
): ViewModel(){
    val tags: StateFlow<List<TagEntity>> =
        repository.getAllTags()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _tagMessage = MutableStateFlow<String?>(null)
    val tagMessage: StateFlow<String?> = _tagMessage
    fun createTag(name: String,colorHex: String){
        viewModelScope.launch {
            val created = repository.createTag(name,colorHex)

            _tagMessage.value = if (created){
                "Tag created successfully."
            } else {
                "Tag name is empty or already exists"
            }
        }
    }

    fun clearTagMessage(){
        _tagMessage.value = null
    }

    fun updateTagColor(tagId: Int, colorHex: String) {
        viewModelScope.launch {
            repository.updateTagColor(tagId, colorHex)
        }
    }
    fun deleteTag(tagId:Int){
        viewModelScope.launch {
            repository.deleteTag(tagId)
        }
    }

    fun addTagToNote(noteId:Int, tagId:Int){
        viewModelScope.launch {
            repository.addTagToNote(noteId,tagId)
        }
    }

    fun removeTagFromNote(noteId:Int, tagId:Int){
        viewModelScope.launch {
            repository.removeTagFromNote(noteId,tagId)
        }
    }

    fun getTagsForNote(noteId:Int): Flow<List<TagEntity>> {
        return repository.getTagsForNote(noteId)
    }

}