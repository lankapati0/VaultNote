package com.lankeshwar.vaultnote.screens.tasks

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.view.RoundedCorner
import android.widget.CheckBox
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.viewmodels.TaskViewModel
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun TaskListScreen(onAddTask:()-> Unit,
                     onEditTask:(TaskEntity)-> Unit,
    taskViewModel: TaskViewModel = hiltViewModel()
    ) {
    val tasks by taskViewModel.tasks.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    val filteredTasks = tasks.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(
            searchQuery,
            ignoreCase = true
        )
    }
    val pendingTasks = filteredTasks.filter { !it.isCompleted }
        .sortedWith(compareBy<TaskEntity> { it.dueDate == null }.thenBy {
            it.dueDate ?: Long.MAX_VALUE
        })

    val completedTasks = filteredTasks.filter { it.isCompleted }.sortedByDescending { it.createdAt }
    val totalTasks = filteredTasks.size

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask
            ) {
                Text("+")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks yet. Add your first task!")
            }
        } else {

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Search Tasks...") }
                )



                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // New Summary Card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total")
                                    Text(
                                        totalTasks.toString(),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                Column {
                                    Text("Pending")
                                    Text(
                                        pendingTasks.size.toString(),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                Column {
                                    Text("Completed")
                                    Text(
                                        completedTasks.size.toString(),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                            }
                        }
                    }

                    item {
                        Text(
                            text = "Pending (${pendingTasks.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(items = pendingTasks, key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onEditTask(task)
                                }) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        taskViewModel.toggleTask(task.id, checked)
                                    }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (task.isCompleted)
                                            TextDecoration.LineThrough
                                        else
                                            TextDecoration.None
                                    )

                                    if (task.description.isNotBlank()) {
                                        Text(task.description)
                                    }

                                    task.dueDate?.let { date ->

                                        val today = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis

                                        val dueDay = Calendar.getInstance().apply {
                                            timeInMillis = date
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis

                                        val isOverdue = !task.isCompleted && dueDay < today

                                        Surface(
                                            color = if (isOverdue)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = (if (isOverdue) "Overdue: " else "Due: ") +
                                                        SimpleDateFormat(
                                                            "dd MMM yyyy",
                                                            Locale.getDefault()
                                                        ).format(Date(date)),
                                                color = if (isOverdue)
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                else
                                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }

                                    }

                                }

                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Completed (${completedTasks.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(items = completedTasks, key = { it.id }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    onEditTask(task)
                                }) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        taskViewModel.toggleTask(task.id, checked)
                                    }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (task.isCompleted)
                                            TextDecoration.LineThrough
                                        else
                                            TextDecoration.None
                                    )

                                    if (task.description.isNotBlank()) {
                                        Text(task.description)
                                    }

                                    task.dueDate?.let { date ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Due: " +
                                                        SimpleDateFormat(
                                                            "dd MMM yyyy",
                                                            Locale.getDefault()
                                                        )
                                                            .format(Date(date)),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

            }
        }

    }

}