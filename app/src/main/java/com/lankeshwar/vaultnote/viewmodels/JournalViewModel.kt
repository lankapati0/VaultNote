package com.lankeshwar.vaultnote.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository
): ViewModel(){
    val journalEntries: StateFlow<List<JournalEntity>> = repository.allEntries.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(5_000)
    )

    fun saveEntry(date: String, title: String,content: String,
                  existingEntry: JournalEntity? = null){
        viewModelScope.launch {
            val entry = existingEntry ?:repository.getEntryByDate(date)
            if(entry == null){
                repository.insertEntity(
                    JournalEntity(date = date,
                        title = title,
                        content = content)
                )
            } else {
                repository.updateEntity(
                    entry.copy(
                        title = title,
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteEntry(entry: JournalEntity){
        viewModelScope.launch {
            repository.deleteEntity(entry)
        }
    }

    suspend fun getEntryByDate(date: String): JournalEntity? {
        return repository.getEntryByDate(date)
    }
    suspend fun getEntryById(id: Int): JournalEntity? {
        return repository.getEntryById(id)
    }

}