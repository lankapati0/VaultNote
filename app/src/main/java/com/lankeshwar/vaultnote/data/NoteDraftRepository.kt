package com.lankeshwar.vaultnote.data

import com.lankeshwar.vaultnote.data.db.NoteDraftDao
import com.lankeshwar.vaultnote.data.db.NoteDraftEntity
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