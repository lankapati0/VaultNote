package com.lankeshwar.vaultnote.screens.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lankeshwar.vaultnote.data.db.entity.JournalEntity
import java.util.Locale

@Composable
fun JournalCalendar(
    entries: List<JournalEntity>,
    onDateSelected:(String) -> Unit
){
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    val today = java.time.LocalDate.now()
    val entryDates = entries.map { it.date }.toSet()
    val firstDayOfMonth = currentMonth.atDay(1)
    val startingDay = firstDayOfMonth.dayOfWeek.value % 7
    val totalDays = currentMonth.lengthOfMonth()
    val totalCells = ((startingDay+ totalDays+6)/7)*7
    val calendarDays:List<Int?> = List(startingDay){null } + (1..totalDays).map { it} + List(totalCells - startingDay-totalDays){null}

    Column(Modifier.fillMaxWidth()) {
        val weekDays = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton (
                onClick = {
                    currentMonth = currentMonth.minusMonths(1)
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = currentMonth.month.getDisplayName(
                    java.time.format.TextStyle.FULL, Locale.getDefault()
                ) + "${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    currentMonth = currentMonth.plusMonths(1)
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }

        }
        Row(Modifier.fillMaxWidth()){
            weekDays.forEach { day ->
                Text(
                    text=day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        calendarDays.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()){
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).height(38.dp),
                        contentAlignment = Alignment.Center) {
                        if(day !=null){
                            val date = currentMonth.atDay(day)
                            val dateString = date.toString()
                            val hasEntry = dateString in entryDates
                            val isToday = date == today

                            Surface(modifier = Modifier.size(30.dp)
                                .clickable{onDateSelected(dateString)},
                                shape = CircleShape,
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.tertiary
                                    hasEntry -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }

                            ) {
                                Box(contentAlignment = Alignment.Center){
                                    Text(
                                        text = day.toString(),
                                        color = when {
                                            isToday -> MaterialTheme.colorScheme.onTertiary
                                            hasEntry -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
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