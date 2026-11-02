package com.lankeshwar.vaultnote.viewmodels

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.data.repository.TaskRepository
import com.lankeshwar.vaultnote.widget.VaultNoteWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context
): ViewModel(){

    val tasks: StateFlow<List<TaskEntity>> =
        repository.getAllTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String, dueDate: Long?) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title,
                    description = description,
                    dueDate = dueDate
                )
            )
            VaultNoteWidget().updateAll(context)
        }
    }

    fun updateTask(task: TaskEntity){
        viewModelScope.launch {
            repository.updateTask(task)
            VaultNoteWidget().updateAll(context)
        }
    }

    fun deleteTask(task: TaskEntity){
        viewModelScope.launch {
            repository.deleteTask(task)
            VaultNoteWidget().updateAll(context)
        }
    }

    fun toggleTask(taskId: Int, isCompleted: Boolean){
        viewModelScope.launch {
            repository.updateTaskStatus(taskId,isCompleted)

            VaultNoteWidget().updateAll(context)
        }
    }
}