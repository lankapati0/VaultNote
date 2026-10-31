package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.db.dao.JournalDao
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JournalRepository @Inject constructor(
    private val journalDao: JournalDao
){
    val allEntries: Flow<List<JournalEntity>> = journalDao.getAllEntries()

    suspend fun insertEntity(entity: JournalEntity){
        journalDao.insertEntry(entity)
    }

    suspend fun updateEntity(entity: JournalEntity){
        journalDao.updateEntry(entity)
    }

    suspend fun deleteEntity(entity: JournalEntity){
        journalDao.deleteEntry(entity)
    }

    suspend fun getEntryByDate(date: String): JournalEntity? {
        return journalDao.getEntryByDate(date)
    }

    suspend fun getEntryById(id:Int): JournalEntity?{
        return journalDao.getEntryById(id)
    }

}