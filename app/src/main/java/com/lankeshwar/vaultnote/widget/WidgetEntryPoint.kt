package com.lankeshwar.vaultnote.widget

import com.lankeshwar.vaultnote.data.db.dao.NoteAttachmentDao
import com.lankeshwar.vaultnote.data.db.dao.NoteDao
import com.lankeshwar.vaultnote.data.db.dao.TaskDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun noteDao(): NoteDao
    fun taskDao(): TaskDao

    fun noteAttachmentDao(): NoteAttachmentDao
}