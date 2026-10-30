package com.lankeshwar.vaultnote.data.repository

import com.lankeshwar.vaultnote.data.db.NoteTagCrossRef
import com.lankeshwar.vaultnote.data.db.dao.TagDao
import com.lankeshwar.vaultnote.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
){
    fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
    }

    suspend fun createTag(name: String,colorHex: String): Boolean{
        val cleanedName = name.trim()
        if(cleanedName.isEmpty()){
            return false
        }
        val existedTags = tagDao.getAllTags().first()

        if(existedTags.any{
            it.name.equals(cleanedName, ignoreCase = true)
            }){
            return false
        }

        return tagDao.insertTag(
            TagEntity(name = cleanedName, colorHex = colorHex)
        ) != -1L
    }

    suspend fun deleteTag(tagId:Int){
        tagDao.removeTagAssignments(tagId)
        tagDao.deleteTag(tagId)
    }

    suspend fun updateTagColor(tagId: Int, colorHex: String) {
        tagDao.updateTagColor(tagId, colorHex)
    }

    suspend fun addTagToNote(noteId:Int, tagId:Int){
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId,tagId = tagId))
    }

    suspend fun removeTagFromNote(noteId: Int,tagId: Int){
        tagDao.removeTagFromNote(noteId,tagId)
    }

    fun getTagsForNote(noteId:Int):Flow<List<TagEntity>>{
        return tagDao.getTagsForNote(noteId)
    }

}