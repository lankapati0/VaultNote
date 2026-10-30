package com.lankeshwar.vaultnote.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lankeshwar.vaultnote.data.db.entity.TaskEntity
import com.lankeshwar.vaultnote.viewmodels.TaskViewModel
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskEditorScreen(task: TaskEntity? = null,
                     onBack:()-> Unit,
                     taskViewModel: TaskViewModel = hiltViewModel()
){

    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var description by remember(task) {mutableStateOf(task?.description ?: "") }
    var dueDate by remember(task){mutableStateOf(task?.dueDate)}

    var showDatePicker by remember {mutableStateOf(false)}
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        TextButton(onClick = {onBack}) {Text("← Back") }
        Text(
            text = if(task == null) "Add Task" else "Edit Task",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = title,
            onValueChange = {title = it},
            label = {Text("Task title")},
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = {description = it},
            label = {Text("Description (optional)")},
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedButton(
            onClick = {
                showDatePicker = true
            }
        ) {
            Text(text = dueDate?.let {
                "Due: ${
                    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(it))
                }"
            } ?: "Select Due Date")
        }

        if(dueDate != null){
            TextButton(
                onClick = {
                    dueDate = null
                }
            ) {
                Text("Remove Due Date")
            }
        }

        if(showDatePicker){
            DatePickerDialog(
                onDismissRequest ={
                    showDatePicker = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dueDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }
                    ) { Text("OK")}
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                        }
                    ) { Text("Cancel")}
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }


        Button(
            onClick = {
                if(title.isNotBlank()){
                    if(task == null){
                        taskViewModel.addTask(title= title, description= description,dueDate = dueDate)
                    } else {
                        taskViewModel.updateTask(
                            task.copy(
                                title = title.trim(),
                                description = description.trim(),
                                dueDate = dueDate
                            )
                        )
                    }
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) {
            Text("Save Task")
        }

        if(task != null){
            OutlinedButton(
                onClick = {
                    showDeleteDialog = true
                }
            ) { Text("Delete Task")}
        }

        if(showDeleteDialog && task!= null){
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                },
                title = {
                    Text(text = "Delete Task")
                },
                text = {
                    Text(text = "Are you sure you want to delete this task?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            taskViewModel.deleteTask(task)
                            showDeleteDialog = false
                            onBack()
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                        }
                    ) { Text("Cancel")}
                }
            )
        }


    }

}