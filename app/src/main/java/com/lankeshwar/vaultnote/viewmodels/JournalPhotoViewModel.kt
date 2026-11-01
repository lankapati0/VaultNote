package com.lankeshwar.vaultnote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.model.JournalPhotoEntity
import com.lankeshwar.vaultnote.data.repository.JournalPhotoRepository
import com.lankeshwar.vaultnote.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalPhotoViewModel @Inject constructor(private val repository: JournalPhotoRepository): ViewModel() {

    fun getPhotosByJournalId(journalId: Int): StateFlow<List<JournalPhotoEntity>>{
        return repository.getPhotosByJournalId(journalId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun insertPhotos(photo: JournalPhotoEntity){
        viewModelScope.launch {
            repository.insertPhotos(photo)
        }
    }

    suspend fun insertPhotoAndWait(photo: JournalPhotoEntity){
        repository.insertPhotos(photo)
    }

    fun deletePhotos(photo: JournalPhotoEntity){
        viewModelScope.launch {
            repository.deletePhotos(photo)
        }
    }

    suspend fun deletePhotoAndWait(photo: JournalPhotoEntity){
        repository.deletePhotos(photo)
    }


}