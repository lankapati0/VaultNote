package com.lankeshwar.vaultnote.screens.journal

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import com.lankeshwar.vaultnote.viewmodels.JournalViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.time.DayOfWeek
import java.time.LocalDate


@Composable
fun JournalScreen(
    journalViewModel: JournalViewModel = hiltViewModel(),
    onAddEntry: (String) -> Unit,
    onEditEntry: (JournalEntity) -> Unit,
    onChooseTemplate:(String)->Unit
) {
    val entries by journalViewModel.journalEntries.collectAsStateWithLifecycle()

    var searchQuery by remember {
        mutableStateOf("")
    }

    val journalDates = entries.map { it.date }.toSet()

    val currentStreak = remember(entries) {
        val today = LocalDate.now()

        var currentDate = if (today.toString() in journalDates) {
            today
        } else {
            today.minusDays(1)
        }

        var streak = 0

        while (currentDate.toString() in journalDates) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        streak
    }

    val weeklyJournalDays = remember(entries) {
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)

        (0..6).map { index ->
            val date = monday.plusDays(index.toLong())
            date to (date.toString() in journalDates)
        }
    }

    val filteredEntries = remember(entries, searchQuery) {
        entries.filter { entry ->
            entry.title.contains(searchQuery, ignoreCase = true) ||
                    entry.content.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Journal heading
        item {
            Row( modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically){
                Text(
                    text = "Your Daily Journal",
                    style = MaterialTheme.typography.headlineSmall
                )
                OutlinedButton(
                    onClick = {
                        onChooseTemplate(LocalDate.now().toString())
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Templates")
                }
            }

        }

        // Current streak card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔥 $currentStreak Day Streak",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (currentStreak == 0) {
                            "Start writing today to build your streak!"
                        } else {
                            "Keep writing every day. You're doing great!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Weekly activity card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weeklyJournalDays.forEach { (date, hasEntry) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.name.take(1),
                                style = MaterialTheme.typography.labelMedium
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                modifier = Modifier.size(30.dp),
                                shape = CircleShape,
                                color = if (hasEntry) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasEntry) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Journal written",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Journal calendar
        item {
            JournalCalendar(
                entries = entries,
                onDateSelected = { selectedDate ->
                    val existingEntry = entries.firstOrNull {
                        it.date == selectedDate
                    }

                    if (existingEntry != null) {
                        onEditEntry(existingEntry)
                    } else {
                        onAddEntry(selectedDate)
                    }
                }
            )
        }

        // Previous entries heading
        item {
            Text(
                text = "Your Previous Entries",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Search journal entries")
                },
                placeholder = {
                    Text("Search by title or content")
                },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                }
            )
        }

        item{
            if(searchQuery.isNotBlank() && entries.isNotEmpty()){
                Text(
                    text = "${filteredEntries.size} journal entries found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Empty journal state
        if (entries.isEmpty()) {
            item {
                Text("No journal entries yet. Start writing today!")
            }
        } else if (filteredEntries.isEmpty()) {
            item {
                Text("No matching journal entries found.")
            }
        } else {

            // Journal history
            items(
                items = filteredEntries,
                key = { it.id }
            ) { entry ->

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
                                SimpleDateFormat(
                                    "EEEE, dd MMMM yyyy",
                                    Locale.getDefault()
                                ).format(
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.getDefault()
                                    ).parse(entry.date)!!
                                )
                            } catch (e: Exception) {
                                entry.date
                            },
                            style = MaterialTheme.typography.labelMedium
                        )

                        Text(
                            text = entry.title.ifBlank {
                                "Untitled Entry"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (entry.mood.isNotBlank()) {
                            Text(
                                text = entry.mood,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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
