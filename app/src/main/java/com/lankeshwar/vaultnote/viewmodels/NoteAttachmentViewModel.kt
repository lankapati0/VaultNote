package com.lankeshwar.vaultnote.viewmodels

import androidx.lifecycle.ViewModel
import com.lankeshwar.vaultnote.data.NoteAttachmentRepository
import com.lankeshwar.vaultnote.data.db.NoteAttachmentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class NoteAttachmentViewModel @Inject constructor(
    private val repository: NoteAttachmentRepository
): ViewModel(){

    fun getAttachmentsForNote(noteId:Int): Flow<List<NoteAttachmentEntity>>{
        return repository.getAttachmentForNote(noteId)
    }
}