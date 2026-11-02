package com.lankeshwar.vaultnote.widget

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lankeshwar.vaultnote.MainActivity
import com.lankeshwar.vaultnote.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class VaultNoteWidget: GlanceAppWidget() {

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val noteDao = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).noteDao()

        val taskDao = EntryPointAccessors.fromApplication(context.applicationContext,
            WidgetEntryPoint::class.java).taskDao()
        val attachmentDao = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).noteAttachmentDao()

        val recentNotes= withContext(Dispatchers.IO){
            noteDao.getRecentNotesForWidget()
        }

        val pendingTasks = withContext(Dispatchers.IO){
            taskDao.getAllTasks().first().filter { !it.isCompleted }.sortedByDescending { it.createdAt }.take(3)
        }
        android.util.Log.d("VaultWidget", "Pending tasks: ${pendingTasks.map { "${it.id}:${it.isCompleted}" }}")
        val attachmentCounts = withContext(Dispatchers.IO){
            recentNotes.associate {note ->
                note.id to attachmentDao.getAttachmentsForNote(note.id).first().size
            }
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.White))
                    .padding(16.dp)
            ) {
                Text(
                    text = "VaultNote",
                    style = TextStyle(
                        color = ColorProvider(Color.Black)
                    )
                )

                Text(
                    text = "Recent Notes",
                    style = TextStyle(
                        color = ColorProvider(Color.Black)
                    )
                )

                if (recentNotes.isEmpty()) {
                    Text(
                        text = "No notes yet",
                        style = TextStyle(
                            color = ColorProvider(Color.DarkGray)
                        )
                    )
                } else {
                    recentNotes.forEach { note ->

                        Column(
                            modifier = GlanceModifier.fillMaxWidth().
                            clickable(
                                actionStartActivity(
                                    componentName = ComponentName(context, MainActivity::class.java),
                                    parameters = actionParametersOf(
                                        ActionParameters.Key<Int>("noteId") to note.id
                                    )
                                )
                            ).padding(8.dp)
                        ) {
                            Text(
                                text = if (note.title.isBlank()) "Untitled" else note.title,
                                style = TextStyle(
                                    color = ColorProvider(Color.Black)
                                )
                            )

                            Text(
                                text = note.preview,
                                style = TextStyle(color = ColorProvider(Color.DarkGray))
                            )

                            val attachmentCount = attachmentCounts[note.id] ?: 0
                            if(attachmentCount > 0){
                                Text(
                                    text = "📎 $attachmentCount ${
                                        if (attachmentCount == 1) "attachment" else "attachments"
                                    }",
                                    style = TextStyle(
                                        color = ColorProvider(Color.DarkGray)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(12.dp))
                Text(
                    text = "Recent Tasks",
                    style = TextStyle(
                        color = ColorProvider(Color.Black)
                    )
                )

                if(pendingTasks.isEmpty()){
                    Text(
                        text = "No pending tasks",
                        style = TextStyle(
                            color = ColorProvider(Color.DarkGray)
                        )
                    )
                } else {
                    pendingTasks.forEach { task ->
                        Column(modifier = GlanceModifier.fillMaxWidth()
                            .clickable(
                             actionStartActivity(
                                 componentName = ComponentName(context, MainActivity::class.java),
                                 parameters = actionParametersOf(
                                     ActionParameters.Key<Int>("taskId") to task.id
                                 )
                             )
                            ).padding(8.dp)) {
                                Text(
                                    text = "☐ ${task.title}",
                                    style = TextStyle(
                                        color = ColorProvider(Color.Black)
                                    )
                                )
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(12.dp))
                Button(
                    text = "＋ Create New Note",
                    onClick = actionStartActivity(
                        componentName = ComponentName(context, MainActivity::class.java),
                        parameters = actionParametersOf(ActionParameters.Key<Boolean>("openCreateNote") to true)
                    )
                )


            }
        }
    }
}