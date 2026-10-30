package com.lankeshwar.vaultnote.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lankeshwar.vaultnote.data.db.NoteTagCrossRef
import com.lankeshwar.vaultnote.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Int)

    @Query("DELETE FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun removeTagAssignments(tagId: Int)

    @Query("UPDATE tags SET colorHex = :colorHex WHERE id = :tagId")
    suspend fun updateTagColor(tagId: Int, colorHex: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToNote(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun removeTagFromNote(noteId:Int, tagId:Int)

    @Query(
        "SELECT tags.* FROM tags " +
                "INNER JOIN note_tag_cross_ref " +
                "ON tags.id = note_tag_cross_ref.tagId " +
                "WHERE note_tag_cross_ref.noteId = :noteId " +
                "ORDER BY tags.name ASC"
    )
    fun getTagsForNote(noteId:Int):Flow<List<TagEntity>>
}