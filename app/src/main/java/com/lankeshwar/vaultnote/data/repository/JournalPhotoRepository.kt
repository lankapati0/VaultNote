package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.model.JournalPhotoDao
import com.lankeshwar.vaultnote.data.model.JournalPhotoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class JournalPhotoRepository @Inject constructor(
    private val journalPhotoDao: JournalPhotoDao
) {

    fun getPhotosByJournalId(journalId:Int): Flow<List<JournalPhotoEntity>>{
        return journalPhotoDao.getPhotosByJournalId(journalId)
    }

    suspend fun insertPhotos(photo: JournalPhotoEntity){
        journalPhotoDao.insertPhotos(photo)
    }

    suspend fun deletePhotos(photo: JournalPhotoEntity){
        journalPhotoDao.deletePhotos(photo)
    }
}