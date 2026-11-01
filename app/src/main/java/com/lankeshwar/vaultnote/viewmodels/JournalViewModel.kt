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

    fun saveEntry(date: String, title: String,content: String,mood: String,
                  existingEntry: JournalEntity? = null){
        viewModelScope.launch {
            val entry = existingEntry ?:repository.getEntryByDate(date)
            if(entry == null){
                repository.insertEntity(
                    JournalEntity(date = date,
                        title = title,
                        content = content, mood = mood)
                )
            } else {
                repository.updateEntity(
                    entry.copy(
                        title = title,
                        content = content, mood = mood,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun saveEntryAndGetId(
        date: String,title:String,content: String,mood: String,
        existingEntry: JournalEntity? = null
    ): Long{
        val entry = existingEntry ?: repository.getEntryByDate(date)

        return if(entry == null){
            repository.insertEntity(
                JournalEntity(
                    date = date, title = title, content = content, mood = mood
                )
            )
        } else {
            repository.updateEntity(
                entry.copy(
                    title = title,
                    content = content,
                    mood = mood,
                    updatedAt = System.currentTimeMillis()
                )
            )
            entry.id.toLong()
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