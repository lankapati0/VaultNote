package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.db.dao.TaskDao
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks()
    }

    suspend fun insertTask(task: TaskEntity){
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity){
        return taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity){
        return taskDao.deleteTask(task)
    }

    suspend fun updateTaskStatus(taskId:Int, isCompleted: Boolean){
        return taskDao.updateTaskStatus(taskId,isCompleted)
    }

}