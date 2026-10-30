package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.db.dao.NoteDraftDao
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteDraftRepository @Inject constructor(
    private val noteDraftDao: NoteDraftDao
){

    suspend fun saveDraft(draft: NoteDraftEntity){
        noteDraftDao.saveDraft(draft)
    }

    suspend fun getDraft(draftId: String): NoteDraftEntity?{
        return noteDraftDao.getDraft(draftId)
    }

    suspend fun deleteDraft(draftId: String){
        noteDraftDao.deleteDraft(draftId)
    }

}