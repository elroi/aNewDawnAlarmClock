package com.elroi.alarmpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImprovedDaySelector(
    selectedDays: List<Int>,
    weekendDays: Set<Int>,
    onSelectionChanged: (List<Int>) -> Unit
) {
    // ISO 8601 indexing to match AndroidAlarmScheduler: Mon=1 … Sat=6, Sun=7
    data class Day(val isoIndex: Int, val label: String)
    val days = listOf(
        Day(7, "Su"), Day(1, "Mo"), Day(2, "Tu"), Day(3, "We"),
        Day(4, "Th"), Day(5, "Fr"), Day(6, "Sa")
    )
    val everyday = listOf(1, 2, 3, 4, 5, 6, 7)
    val weekend  = everyday.filter { weekendDays.contains(it) }.sorted()
    val weekdays = everyday.filter { !weekendDays.contains(it) }.sorted()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick-select row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "Weekdays"  to weekdays,
                "Weekend"   to weekend,
                "Every day" to everyday
            ).forEach { (label, target) ->
                val isActive = selectedDays.sorted() == target.sorted()
                Surface(
                    selected = isActive,
                    onClick = { onSelectionChanged(if (isActive) emptyList() else target) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Day chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            days.forEach { day ->
                val isSelected = selectedDays.contains(day.isoIndex)
                Surface(
                    selected = isSelected,
                    onClick = {
                        val new = if (isSelected) selectedDays - day.isoIndex else selectedDays + day.isoIndex
                        onSelectionChanged(new.sorted())
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = day.label,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
