package com.lankeshwar.vaultnote.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.platform.LocalLocale

@Composable
fun JournalScreen(
    journalViewModel: JournalViewModel = hiltViewModel(),
    onAddEntry:(String) -> Unit,
    onEditEntry:(JournalEntity)-> Unit
) {
    val entries by journalViewModel.journalEntries.collectAsStateWithLifecycle()
    var selectedDate by remember {mutableStateOf(
        SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date())
    )}

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Your Daily Journal",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your Previous Entries",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        JournalCalendar(entries = entries,
            onDateSelected = { selectedDate ->
                val existingEntry = entries.firstOrNull(){
                    it.date == selectedDate
                }
                if(existingEntry != null){
                    onEditEntry(existingEntry)
                } else {
                    onAddEntry(selectedDate)
                }
            })
        Spacer(modifier = Modifier.height(16.dp))


        if (entries.isEmpty()) {
            Text("No journal entries yet. Start writing today!")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items = entries, key = { it.id }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEditEntry(entry)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = try {
                                    java.text.SimpleDateFormat(
                                        "EEEE, dd MMMM yyyy",
                                        java.util.Locale.getDefault()
                                    ).format(
                                        java.text.SimpleDateFormat(
                                            "yyyy-MM-dd",
                                            java.util.Locale.getDefault()
                                        ).parse(entry.date)!!
                                    )
                                }catch (e: Exception){
                                    entry.date
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = entry.title.ifBlank { "Untitled Entry" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = entry.content,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}