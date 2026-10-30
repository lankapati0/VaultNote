package com.lankeshwar.vaultnote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lankeshwar.vaultnote.data.db.entity.NoteDraftEntity

@Dao
interface NoteDraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: NoteDraftEntity)

    @Query("SELECT * FROM note_drafts WHERE draftId = :draftId LIMIT 1")
    suspend fun getDraft(draftId: String): NoteDraftEntity?

    @Query("DELETE FROM note_drafts WHERE draftId = :draftId")
    suspend fun deleteDraft(draftId: String)

}